package com.google.code.twig.standard;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.appengine.api.datastore.AsyncPreparedQuery;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;
import com.google.code.twig.FindCommand;
import com.google.code.twig.FindCommand.BranchFindCommand;
import com.google.code.twig.FindCommand.ChildFindCommand;
import com.google.code.twig.FindCommand.MergeOperator;
import com.google.code.twig.FindCommand.TypedFindCommand;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;

abstract class StandardTypedFindCommand<T, C extends TypedFindCommand<T, C>> extends StandardCommonFindCommand<T, C> implements TypedFindCommand<T, C>, BranchFindCommand<T>
{
	protected List<StandardBranchFindCommand<T>> children;
	protected List<Filter> filters;
	private MergeOperator operator;

	private static class Filter implements Serializable
	{
		private static final long serialVersionUID = 1L;
		
		@SuppressWarnings("unused")
		private Filter()
		{
		}
		
		public Filter(String field, FilterOperator operator, Object value)
		{
			this.field = field;
			this.operator = operator;
			this.value = value;
		}
		
		String field;
		FilterOperator operator;
		Object value;
	}

	StandardTypedFindCommand(StrategyObjectDatastore datastore)
	{
		super(datastore);
	}

	protected abstract Query newQuery();

	abstract StandardRootFindCommand<T> getRootCommand();

	@SuppressWarnings("unchecked")
	public C addFilter(String field, FilterOperator operator, Object value)
	{
		if (filters == null)
		{
			filters = new ArrayList<Filter>(2);
		}
		filters.add(new Filter(field, operator, value));
		return (C) this;
	}
	
	@SuppressWarnings("unchecked")
	public C addRangeFilter(String field, Object from, Object to)
	{
		addFilter(field, FilterOperator.GREATER_THAN_OR_EQUAL, from);
		addFilter(field, FilterOperator.LESS_THAN, to);
		return (C) this;
	}

	public BranchFindCommand<T> branch(FindCommand.MergeOperator operator)
	{
		if (this.operator != null)
		{
			throw new IllegalStateException("Can only branch a command once");
		}
		this.operator = operator;
		return this;
	}
	
	public ChildFindCommand<T> addChildCommand()
	{
		StandardBranchFindCommand<T> child = new StandardBranchFindCommand<T>(this);
		if (children == null)
		{
			children = new ArrayList<StandardBranchFindCommand<T>>(2);
		}
		children.add(child);		
		return child;
	}

	protected Collection<Query> queries()
	{
		if (children == null)
		{
			return Collections.singleton(newQuery());
		}
		else
		{
			List<Query> queries = new ArrayList<Query>(children.size() * 2);
			for (StandardBranchFindCommand<T> child : children)
			{
				queries.addAll(child.queries());
			}
			return queries;
		}
	}

	protected Collection<Query> getValidatedQueries()
	{
		Collection<Query> queries = queries();
		if (queries.iterator().next().isKeysOnly() && (entityRestriction != null || propertyRestriction != null))
		{
			throw new IllegalStateException("Cannot set filters for a keysOnly query");
		}
		return queries;
	}

	void applyFilters(Query query)
	{
		if (filters != null)
		{
			for (Filter filter : filters)
			{
				query.addFilter(filter.field, filter.operator, filter.value);
			}
		}
	}

	Future<? extends Iterator<Entity>> futureEntityIterator()
	{
		Collection<Query> queries = queries();
		if (queries.size() == 1)
		{
			return futureSingleQueryEntities(queries.iterator().next());
		}
		else
		{
			assert queries.isEmpty() == false;
			return futureMergedEntities(queries);
		}
	}

	protected QueryResultIterator<Entity> nowSingleQueryEntities(Query query)
	{
		final QueryResultIterator<Entity> entities;
		PreparedQuery prepared = this.datastore.servicePrepare(query);
		FetchOptions fetchOptions = getRootCommand().getFetchOptions();
		if (fetchOptions == null)
		{
			entities = prepared.asQueryResultIterator();
		}
		else
		{
			entities = prepared.asQueryResultIterator(fetchOptions);
		}
		return entities;
	}

	Future<QueryResultIterator<Entity>> futureSingleQueryEntities(Query query)
	{
			Transaction txn = this.datastore.getTransaction();
			Future<QueryResultIterator<Entity>> futureEntities;
			AsyncPreparedQuery prepared = new AsyncPreparedQuery(query, txn);
			FetchOptions fetchOptions = getRootCommand().getFetchOptions();
			if (fetchOptions == null)
			{
				futureEntities = prepared.asFutureQueryResultIterator();
			}
			else
			{
				futureEntities = prepared.asFutureQueryResultIterator(fetchOptions);
			}
			return futureEntities;
	}

	protected Iterator<Entity> nowMultipleQueryEntities(Collection<Query> queries)
	{
		List<Iterator<Entity>> iterators = new ArrayList<Iterator<Entity>>(queries.size());
		for (Query query : queries)
		{
			PreparedQuery prepared = this.datastore.servicePrepare(query);
			Iterator<Entity> entities;
			FetchOptions fetchOptions = getRootCommand().getFetchOptions();
			if (fetchOptions == null)
			{
				entities = prepared.asIterator();
			}
			else
			{
				entities = prepared.asIterator(fetchOptions);
			}

			// apply filters etc
			entities = applyEntityFilter(entities);
			iterators.add(entities);
		}

		// all queries have the same sorts
		Query query = queries.iterator().next();
		List<SortPredicate> sorts = query.getSortPredicates();
		Iterator<Entity> merged = mergeEntities(iterators, sorts);
		return merged;
	}

	<R> Future<Iterator<R>> futureMultiQueryInstanceIterator()
	{
		Collection<Query> queries = getValidatedQueries();

			return futureMultipleQueriesInstanceIterator(queries);
	}

	protected <R> Iterator<R> nowMultiQueryInstanceIterator()
	{
		try
		{
			Collection<Query> queries = getValidatedQueries();
			Iterator<Entity> entities = nowMultipleQueryEntities(queries);
			return entitiesToInstances(entities, propertyRestriction);
		}
		catch (Exception e)
		{
			// only unchecked exceptions thrown from datastore service
			throw (RuntimeException) e.getCause();
		}
	}

	private <R> Future<Iterator<R>> futureMultipleQueriesInstanceIterator(Collection<Query> queries)
	{
		final Future<Iterator<Entity>> futureMerged = futureMergedEntities(queries);

		return new Future<Iterator<R>>()
		{
			public boolean cancel(boolean mayInterruptIfRunning)
			{
				return futureMerged.cancel(mayInterruptIfRunning);
			}

			public Iterator<R> get() throws InterruptedException, ExecutionException
			{
				return entitiesToInstances(futureMerged.get(), propertyRestriction);
			}

			public Iterator<R> get(long timeout, TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException
			{
				return entitiesToInstances(futureMerged.get(timeout, unit), propertyRestriction);
			}

			public boolean isCancelled()
			{
				return futureMerged.isCancelled();
			}

			public boolean isDone()
			{
				return futureMerged.isDone();
			}
		};

	}

	private Future<Iterator<Entity>> futureMergedEntities(Collection<Query> queries)
	{
		List<Future<QueryResultIterator<Entity>>> futures = multiQueriesToFutureEntityIterators(queries);
		Query query = queries.iterator().next();
		List<SortPredicate> sorts = query.getSortPredicates();
		return futureEntityIteratorsToFutureMergedIterator(futures, sorts);
	}

	protected List<Future<QueryResultIterator<Entity>>> multiQueriesToFutureEntityIterators(
			Collection<Query> queries)
	{
		final List<Future<QueryResultIterator<Entity>>> futures = new ArrayList<Future<QueryResultIterator<Entity>>>(queries.size());
		Transaction txn = datastore.getTransaction();
		for (Query query : queries)
		{
			AsyncPreparedQuery prepared = new AsyncPreparedQuery(query, txn);
			Future<QueryResultIterator<Entity>> futureEntities;
			FetchOptions fetchOptions = getRootCommand().getFetchOptions();
			if (fetchOptions == null)
			{
				futureEntities = prepared.asFutureQueryResultIterator();
			}
			else
			{
				futureEntities = prepared.asFutureQueryResultIterator(fetchOptions);
			}
			futures.add(futureEntities);
		}
		return futures;
	}

	private Future<Iterator<Entity>> futureEntityIteratorsToFutureMergedIterator(
			final List<Future<QueryResultIterator<Entity>>> futures, final List<SortPredicate> sorts)
	{
		return new Future<Iterator<Entity>>()
		{

			public boolean cancel(boolean mayInterruptIfRunning)
			{
				boolean success = true;
				for (Future<QueryResultIterator<Entity>> future : futures)
				{
					if (future.cancel(mayInterruptIfRunning) == false)
					{
						success = false;
					}
				}
				return success;
			}

			public Iterator<Entity> get() throws InterruptedException, ExecutionException
			{
				return futureQueriesToEntities(futures);
			}

			public Iterator<Entity> get(long timeout, TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException
			{
				return futureQueriesToEntities(futures);
			}

			private Iterator<Entity> futureQueriesToEntities(
					List<Future<QueryResultIterator<Entity>>> futures)
					throws InterruptedException, ExecutionException
			{
				List<Iterator<Entity>> iterators = new ArrayList<Iterator<Entity>>(futures.size());
				for (Future<QueryResultIterator<Entity>> future : futures)
				{
					Iterator<Entity> entities = future.get();
					entities = applyEntityFilter(entities);
					iterators.add(entities);
				}
				return mergeEntities(iterators, sorts);
			}

			public boolean isCancelled()
			{
				// only if all are canceled
				for (Future<QueryResultIterator<Entity>> future : futures)
				{
					if (!future.isCancelled())
					{
						return false;
					}
				}
				return true;
			}

			public boolean isDone()
			{
				// only if all are done
				for (Future<QueryResultIterator<Entity>> future : futures)
				{
					if (!future.isDone())
					{
						return false;
					}
				}
				return true;
			}
		};
	}

//	private final class KeyToInstanceFunction<T> implements Function<Entity, T>
//	{
//		private final Predicate<String> propertyPredicate;
//
//		public KeyToInstanceFunction(Predicate<String> propertyPredicate)
//		{
//			this.propertyPredicate = propertyPredicate;
//		}
//
//		public T apply(Entity entity)
//		{
//			@SuppressWarnings("unchecked")
//			T result = (T) datastore.keyToInstance(entity.getKey(), propertyPredicate);
//			return result;
//		}
//	}
//
//	private final class ParentKeyToInstanceFunction<T> implements Function<Entity, T>
//	{
//		private final Predicate<String> propertyPredicate;
//
//		public ParentKeyToInstanceFunction(Predicate<String> propertyPredicate)
//		{
//			this.propertyPredicate = propertyPredicate;
//		}
//
//		public T apply(Entity entity)
//		{
//			@SuppressWarnings("unchecked")
//			T result = (T) datastore.keyToInstance(entity.getKey().getParent(), propertyPredicate);
//			return result;
//		}
//	}
	public class FilteredIterator<V> extends AbstractIterator<V>
	{
		private final Iterator<V> unfiltered;
		private final Predicate<V> predicate;

		public FilteredIterator(Iterator<V> unfiltered, Predicate<V> predicate)
		{
			this.unfiltered = unfiltered;
			this.predicate = predicate;
		}

		@Override
		protected V computeNext()
		{
			while (unfiltered.hasNext())
			{
				V next = unfiltered.next();
				if (predicate.apply(next))
				{
					return next;
				}
			}
			return endOfData();
		}
	}

}
