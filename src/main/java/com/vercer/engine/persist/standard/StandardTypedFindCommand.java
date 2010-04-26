package com.vercer.engine.persist.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.AsyncDatastoreHelper;
import com.google.appengine.api.datastore.AsyncPreparedQuery;
import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.api.utils.FutureWrapper;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.Iterators;
import com.vercer.engine.persist.FindCommand.BranchFindCommand;
import com.vercer.engine.persist.FindCommand.ParentsCommand;
import com.vercer.engine.persist.FindCommand.TypedFindCommand;
import com.vercer.engine.persist.util.SortedMergeIterator;

public abstract class StandardTypedFindCommand<T, C extends TypedFindCommand<T, C>> extends StandardBaseFindCommand<T, C> implements TypedFindCommand<T, C>
{
	private static final Logger log = Logger.getLogger(StandardTypedFindCommand.class.getName());

	protected List<StandardBranchFindCommand<T>> children;
	protected List<Filter> filters;
	
	// TODO remove this! Written for testing but might keep something similar to force
	// sync queries. So just keeping it here for now
	public static boolean forceMultipleNow;

	private class Filter
	{
		public Filter(String field, FilterOperator operator, Object value)
		{
			super();
			this.field = field;
			this.operator = operator;
			this.value = value;
		}
		String field;
		FilterOperator operator;
		Object value;
	}

	public StandardTypedFindCommand(StrategyObjectDatastore datastore)
	{
		super(datastore);
	}

	protected abstract Query newQuery();

	protected abstract FetchOptions getFetchOptions();

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

	public BranchFindCommand<T> addChildQuery()
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
		if (queries.iterator().next().isKeysOnly() && (entityPredicate != null || propertyPredicate != null))
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

	public <P> Iterator<P> returnParentsNow()
	{
		return this.<P>returnParentsCommandNow().returnParentsNow();
	}

	Iterator<Entity> childEntitiesToParentEntities(Iterator<Entity> childEntities)
	{
		childEntities = applyEntityFilter(childEntities);
		@SuppressWarnings("deprecation")
		int chunkSize = FetchOptions.DEFAULT_CHUNK_SIZE;
		FetchOptions fetchOptions = getFetchOptions();
		if (fetchOptions != null)
		{
			chunkSize = fetchOptions.getChunkSize();
		}
		Iterator<Entity> parentEntities = new PrefetchParentIterator(childEntities, datastore, chunkSize);
		return parentEntities;
	}

	public <P> ParentsCommand<P> returnParentsCommandNow()
	{
		Collection<Query> queries = queries();
		if (queries.size() == 1)
		{
			Iterator<Entity> childEntities = nowSingleQueryEntities(queries.iterator().next());
			childEntities = applyEntityFilter(childEntities);
			return new StandardSingleParentsCommand<P>(this, childEntities);
		}
		else
		{

			try
			{
				List<Iterator<Entity>> childIterators = new ArrayList<Iterator<Entity>>(queries.size());
				long start = System.currentTimeMillis();
				if (forceMultipleNow)  // just for performance testing async vs sync keys only
				{
					for (Query query : queries)
					{
						Iterator<Entity> iterator = datastore.servicePrepare(query).asIterator();
						childIterators.add(iterator);
					}
					log.info("Now " + (System.currentTimeMillis() - start));
				}
				else
				{
					List<Future<QueryResultIterator<Entity>>> futures = multiQueriesToFutureEntityIterators(queries);
					for (Future<QueryResultIterator<Entity>> future : futures)
					{
						childIterators.add(future.get());
					}
					log.info("Future " + (System.currentTimeMillis() - start));
				}

				Query query = queries.iterator().next();
				List<SortPredicate> sorts = query.getSortPredicates();
				if (query.isKeysOnly() == false)
				{
					// we should have the property values from the sort to merge
					Iterator<Entity> childEntities = mergeEntities(childIterators, sorts);
					childEntities = applyEntityFilter(childEntities);
					return new StandardSingleParentsCommand<P>(this, childEntities);
				}
				else
				{
					return new StandardMergeParentsCommand<P>(this, childIterators, sorts);
				}
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <P> Future<ParentsCommand<P>> returnParentsCommandLater()
	{
		Future<Iterator<Entity>> futureEntityIterator = (Future<Iterator<Entity>>) futureEntityIterator();
		return new FutureWrapper<Iterator<Entity>, ParentsCommand<P>>(futureEntityIterator)
		{
			@Override
			protected Throwable convertException(Throwable arg0)
			{
				return arg0;
			}

			@Override
			protected ParentsCommand<P> wrap(Iterator<Entity> childEntities) throws Exception
			{
				return new StandardSingleParentsCommand(StandardTypedFindCommand.this, childEntities);
			}
		};
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

	// TODO get rid of this
	<R> QueryResultIterator<R> nowSingleQueryInstanceIterator()
	{
		Collection<Query> queries = getValidatedQueries();
		if (queries.size() > 1)
		{
			throw new IllegalStateException("Too many queries");
		}
		Query query = queries.iterator().next();

		QueryResultIterator<Entity> entities = nowSingleQueryEntities(query);

		Iterator<Entity> iterator = applyEntityFilter(entities);

		Iterator<R> instances = entityToInstanceIterator(iterator, query.isKeysOnly());
		return new BasicQueryResultIterator<R>(instances, entities);
	}

	private QueryResultIterator<Entity> nowSingleQueryEntities(Query query)
	{
		final QueryResultIterator<Entity> entities;
		PreparedQuery prepared = this.datastore.servicePrepare(query);
		FetchOptions fetchOptions = getFetchOptions();
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

	<R> Future<QueryResultIterator<R>> futureSingleQueryInstanceIterator()
	{
		Collection<Query> queries = getValidatedQueries();
		if (queries.size() > 1)
		{
			throw new IllegalStateException("Multiple queries defined");
		}

		final Query query = queries.iterator().next();
		final Future<QueryResultIterator<Entity>> futureEntities = futureSingleQueryEntities(query);

		return new Future<QueryResultIterator<R>>()
		{
			private QueryResultIterator<R> doGet(QueryResultIterator<Entity> entities)
			{
					Iterator<Entity> iterator = applyEntityFilter(entities);
					Iterator<R> instances = entityToInstanceIterator(iterator, query.isKeysOnly());
					return new BasicQueryResultIterator<R>(instances, entities);
			}

			public QueryResultIterator<R> get() throws InterruptedException,
					ExecutionException
			{
					return doGet(futureEntities.get());
			}

			public QueryResultIterator<R> get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException,
					TimeoutException
			{
					return doGet(futureEntities.get(timeout, unit));
			}

			public boolean isCancelled()
			{
				return futureEntities.isCancelled();
			}

			public boolean isDone()
			{
				return futureEntities.isDone();
			}
			public boolean cancel(boolean mayInterruptIfRunning)
			{
				return futureEntities.cancel(mayInterruptIfRunning);
			}
		};
	}

	private Future<QueryResultIterator<Entity>> futureSingleQueryEntities(Query query)
	{
			Transaction txn = this.datastore.getTransaction();
			Future<QueryResultIterator<Entity>> futureEntities;
			AsyncPreparedQuery prepared = new AsyncPreparedQuery(query, txn);
			FetchOptions fetchOptions = getFetchOptions();
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
//
//	private boolean isInternalIncompatability(Throwable t)
//	{
//		return t instanceof Error || 
//		t instanceof SecurityException || 
//		t instanceof ClassNotFoundException ||
//		t instanceof NoSuchMethodException;
//	}

	protected Iterator<Entity> nowMultipleQueryEntities(Collection<Query> queries)
	{
		List<Iterator<Entity>> iterators = new ArrayList<Iterator<Entity>>(queries.size());
		for (Query query : queries)
		{
			PreparedQuery prepared = this.datastore.servicePrepare(query);
			Iterator<Entity> entities;
			FetchOptions fetchOptions = getFetchOptions();
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
			return entityToInstanceIterator(entities, false);
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
		final boolean keysOnly = queries.iterator().next().isKeysOnly();

		return new Future<Iterator<R>>()
		{
			public boolean cancel(boolean mayInterruptIfRunning)
			{
				return futureMerged.cancel(mayInterruptIfRunning);
			}

			public Iterator<R> get() throws InterruptedException, ExecutionException
			{
					return entityToInstanceIterator(futureMerged.get(), keysOnly);
			}

			public Iterator<R> get(long timeout, TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException
			{
					return entityToInstanceIterator(futureMerged.get(timeout, unit), keysOnly);
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

	private List<Future<QueryResultIterator<Entity>>> multiQueriesToFutureEntityIterators(
			Collection<Query> queries)
	{
		final List<Future<QueryResultIterator<Entity>>> futures = new ArrayList<Future<QueryResultIterator<Entity>>>(queries.size());
		Transaction txn = this.datastore.getTransaction();
		for (Query query : queries)
		{
			AsyncPreparedQuery prepared = new AsyncPreparedQuery(query, txn);
			Future<QueryResultIterator<Entity>> futureEntities;
			FetchOptions fetchOptions = getFetchOptions();
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

	Iterator<Entity> mergeEntities(List<Iterator<Entity>> iterators, List<SortPredicate> sorts)
	{
		Iterator<Entity> merged;
		if (sorts != null && !sorts.isEmpty())
		{
			Comparator<Entity> comparator = AsyncDatastoreHelper.newEntityComparator(sorts);
			merged = new SortedMergeIterator<Entity>(comparator, iterators, true);
		}
		else
		{
			merged = Iterators.concat(iterators.iterator());
		}
		return merged;
	}


	<R> Iterator<R> entityToInstanceIterator(Iterator<Entity> entities, boolean keysOnly)
	{
		Function<Entity, R> function = new EntityToInstanceFunction<R>(this.propertyPredicate);
		return Iterators.transform(entities, function);
	}

	private final class EntityToInstanceFunction<R> implements Function<Entity, R>
	{
		private final Predicate<String> predicate;

		public EntityToInstanceFunction(Predicate<String> predicate)
		{
			this.predicate = predicate;
		}

		@SuppressWarnings("unchecked")
		public R apply(Entity entity)
		{
			return (R) datastore.toTypesafe(entity, predicate);
		}
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

	private class BasicQueryResultIterator<V> extends ForwardingIterator<V> implements QueryResultIterator<V>
	{
		private final Iterator<V> instances;
		private final QueryResultIterator<Entity> entities;

		public BasicQueryResultIterator(Iterator<V> instances, QueryResultIterator<Entity> entities)
		{
			this.instances = instances;
			this.entities = entities;
		}

		@Override
		protected Iterator<V> delegate()
		{
			return instances;
		}

		public Cursor getCursor()
		{
			return entities.getCursor();
		}
	}
	public class ParentEntityIterator implements Iterator<Entity>
	{
		private final Iterator<Entity> children;

		public ParentEntityIterator(Iterator<Entity> entities)
		{
			this.children = entities;
		}

		public boolean hasNext()
		{
			return children.hasNext();
		}

		public Entity next()
		{
			return datastore.keyToInstance(children.next().getKey(), propertyPredicate);
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}

}
