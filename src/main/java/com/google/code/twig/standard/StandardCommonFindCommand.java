package com.google.code.twig.standard;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
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
import com.google.code.twig.FindCommand.CommonFindCommand;
import com.google.code.twig.FindCommand.MergeOperator;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.Pair;
import com.google.code.twig.util.Reflection;
import com.google.code.twig.util.Strings;
import com.google.code.twig.util.generic.Generics;
import com.google.code.twig.util.reference.ObjectReference;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;

abstract class StandardCommonFindCommand<C extends CommonFindCommand<C>> extends StandardRestrictedFindCommand<C> implements CommonFindCommand<C>, BranchFindCommand
{
	protected List<StandardBranchFindCommand> children;
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

	StandardCommonFindCommand(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}

	protected abstract Query newQuery();

	abstract StandardRootFindCommand<?> getRootCommand();

	@SuppressWarnings("unchecked")
	public C addFilter(String fieldPathName, FilterOperator operator, Object value)
	{		
		Pair<Field, String> fieldAndProperty = getFieldAndPropertyForPath(fieldPathName);
		Field field = fieldAndProperty.getFirst();
		String property = fieldAndProperty.getSecond();
		
		PropertyTranslator translator = datastore.encoder(field, value);
		
		// for IN we need to encode each value of the collection
		Object encoded;
		if (Entity.KEY_RESERVED_PROPERTY.equals(property))
		{
			// this is an @id field so we need to create a Key value
			String kind = datastore.getConfiguration().typeToKind(getRootCommand().getType());
			encoded = StandardCommonLoadCommand.idToKey(value, field, kind, datastore, null);
		}
		else
		{
			// the property must be a path string
			if (operator == FilterOperator.IN)
			{
				Collection<?> values = (Collection<?>) value;
				Collection<Object> encodeds = new ArrayList<Object>(values.size()); 
				for (Object item : values)
				{
					encodeds.add(encodeFieldValue(translator, item, field, new Path.Builder(property).build()));
				}
				encoded = encodeds;
			}
			else
			{
				encoded = encodeFieldValue(translator, value, field, new Path.Builder(property).build());
			}
			
		}
		
		addDirectFilter(property, operator, encoded);
		
		return (C) this;
	}
	
	// TODO user this for sort fields also
	protected Pair<Field, String> getFieldAndPropertyForPath(String fieldPathName)
	{
		Type type = getRootCommand().getType();
		Field field = null;
		
		// get the stored path from the object navigation path
		String[] fieldNames = Strings.split(fieldPathName, false, '.');
		Path path = Path.EMPTY_PATH;
		String property = null;
		for (String fieldName : fieldNames)
		{
			field = null;
			Class<?> erased = Generics.erase(type);
	
			// collections use the element type
			if (Collection.class.isAssignableFrom(erased))
			{
				type = ((ParameterizedType) Generics.getExactSuperType(type, Collection.class)).getActualTypeArguments()[0];
				erased = Generics.erase(type);
			}
			
			// get fields that were already cached in any order
			// TODO cache fields? need to take timings. probably not worth it for filters
			Collection<Field> fields = Reflection.getAccessibleFields(erased);
			for (Field candidate : fields)
			{
				if (candidate.getName().equals(fieldName))
				{
					field = candidate;
				}
			}
			
			if (field == null)
			{
				throw new IllegalArgumentException("Could not find field " + fieldName + " in type " + type);
			}
			
			// field type could have type variable if defined in superclass
			type = Generics.getExactFieldType(field, type);
	
			// if the field is an @Id we need to create a Key value
			if (datastore.getConfiguration().id(field))
			{
				if (!path.isEmpty())
				{
					throw new IllegalArgumentException("Id field must be at root of filter");
				}
				property = Entity.KEY_RESERVED_PROPERTY;
				break;
			}
			
			// the property name stored in the datastore may use a short name
			String propertyName = datastore.getConfiguration().name(field);
			path = new Path.Builder(path).field(propertyName).build();
		}
		
		// path will only be empty if we are filtering on id
		if (!path.isEmpty())
		{
			assert property == null; 
			property = path.toString();
		}
		
		return new Pair<Field, String>(field, property);
	}
	
	@SuppressWarnings("unchecked")
	// by passes the search for object field in root type
	public C addDirectFilter(String property, FilterOperator operator, Object value)
	{
		if (filters == null)
		{
			filters = new ArrayList<Filter>(2);
		}
		
		filters.add(new Filter(property, operator, value));
		
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	private Object encodeFieldValue(PropertyTranslator translator, Object value, Field field, Path path)
	{
		Set<Property> properties = translator.encode(value, path, true);
		if (properties == null)
		{
			throw new IllegalArgumentException("Could not encode value " + value + " for field " + field);
		}
		
		if (properties.size() != 1)
		{
			throw new IllegalArgumentException("Encoder for field " + field + " must return one value for " + value + " but returned " + properties);
		}
		
		// can only have one value for a filter (ex IN)
		Object encoded = properties.iterator().next().getValue();
		
		// is this a reference to a key which we should have already
		if (encoded instanceof ObjectReference<?>)
		{
			// cannot dereference as can store instance
			encoded = datastore.associatedKey(value);
			
			if (encoded == null)
			{
				throw new IllegalArgumentException("Could not find related instance " + value);
			}
		}
		return encoded;
	}
	
	@SuppressWarnings("unchecked")
	public C addRangeFilter(String field, Object from, Object to)
	{
		addFilter(field, FilterOperator.GREATER_THAN_OR_EQUAL, from);
		addFilter(field, FilterOperator.LESS_THAN, to);
		return (C) this;
	}

	public BranchFindCommand branch(FindCommand.MergeOperator operator)
	{
		if (this.operator != null)
		{
			throw new IllegalStateException("Can only branch a command once");
		}
		this.operator = operator;
		return this;
	}
	
	public ChildFindCommand addChildCommand()
	{
		StandardBranchFindCommand child = new StandardBranchFindCommand(this);
		if (children == null)
		{
			children = new ArrayList<StandardBranchFindCommand>(2);
		}
		children.add(child);		
		return child;
	}

	// TODO put many methods like this into SRFC
	protected Collection<Query> queries()
	{
		if (children == null)
		{
			return Collections.singleton(newQuery());
		}
		else
		{
			List<Query> queries = new ArrayList<Query>(children.size() * 2);
			for (StandardBranchFindCommand child : children)
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
