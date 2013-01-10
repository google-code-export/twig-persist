package com.google.code.twig.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.code.twig.CommandTerminator;
import com.google.code.twig.FindCommand.ParentsCommand;
import com.google.code.twig.FindCommand.RootFindCommand;
import com.google.code.twig.LoadCommand.CacheMode;
import com.google.code.twig.util.FutureAdaptor;
import com.google.code.twig.util.ImmediateFuture;
import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.Lists;

public class StandardRootFindCommand<T> extends StandardCommonFindCommand<StandardRootFindCommand<T>>
		implements RootFindCommand<T>
{
	private final Class<?> type;
	private FetchOptions options;
	private Object ancestor;
	List<Sort> sorts;

	class Sort
	{
		public Sort(String field, SortDirection direction)
		{
			super();
			this.direction = direction;
			this.field = field;
		}

		SortDirection direction;
		String field;
	}

	protected StandardRootFindCommand(Class<?> type, TranslatorObjectDatastore datastore, int initialActivationDepth)
	{
		super(datastore, initialActivationDepth);
		this.type = type;
	}

	@Override
	StandardRootFindCommand<T> getRootCommand()
	{
		return this;
	}
	
	Class<?> getType()
	{
		return type;
	}

	@Override
	public StandardRootFindCommand<T> ancestor(Object ancestor)
	{
		this.ancestor = ancestor;
		return this;
	}
	
	@Override
	public StandardRootFindCommand<T> addSort(String field)
	{
		return addSort(field, SortDirection.ASCENDING);
	}

	@Override
	public StandardRootFindCommand<T> remember()
	{
		this.remember = true;
		return this;
	}
	
	@Override
	public StandardRootFindCommand<T> addSort(String field, SortDirection direction)
	{
		if (this.sorts == null)
		{
			this.sorts = new ArrayList<Sort>(2);
		}
		this.sorts.add(new Sort(field, direction));
		return this;
	}

	@Override
	public StandardRootFindCommand<T> continueFrom(Cursor cursor)
	{
		if (this.options == null)
		{
			this.options = FetchOptions.Builder.withDefaults();
		}
		this.options.startCursor(cursor);
		return this;
	}

	@Override
	public StandardRootFindCommand<T> finishAt(Cursor cursor)
	{
		if (this.options == null)
		{
			this.options = FetchOptions.Builder.withDefaults();
		}
		this.options.endCursor(cursor);
		return this;
	}

	@Override
	public StandardRootFindCommand<T> fetchNextBy(int size)
	{
		if (this.options == null)
		{
			this.options = FetchOptions.Builder.withChunkSize(size);
		}
		else
		{
			this.options.chunkSize(size);
		}
		return this;
	}

	@Override
	public StandardRootFindCommand<T> fetchFirst(int size)
	{
		if (this.options == null)
		{
			this.options = FetchOptions.Builder.withPrefetchSize(size);
		}
		else
		{
			this.options.prefetchSize(size);
		}
		return this;
	}

	@Override
	public StandardRootFindCommand<T> startFrom(int offset)
	{
		if (this.options == null)
		{
			this.options = FetchOptions.Builder.withOffset(offset);
		}
		else
		{
			this.options.offset(offset);
		}
		return this;
	}

	@Override
	public StandardRootFindCommand<T> fetchMaximum(int limit)
	{
		if (this.options == null)
		{
			this.options = FetchOptions.Builder.withLimit(limit);
		}
		else
		{
			this.options.limit(limit);
		}
		return this;
	}

	@Override
	public Future<QueryResultIterator<T>> later()
	{
		return new ImmediateFuture<QueryResultIterator<T>>(now());
	}

	@Override
	public CommandTerminator<Integer> returnCount()
	{
		return new CommandTerminator<Integer>()
		{
			@Override
			public Integer now()
			{
				Collection<Query> queries = getValidatedQueries();
				if (queries.size() > 1)
				{
					throw new IllegalStateException("Too many queries");
				}

				Query query = queries.iterator().next();
				PreparedQuery prepared = datastore.servicePrepare(query, getSettings());
				if (options == null)
				{
					options = FetchOptions.Builder.withDefaults();
				}
				return prepared.countEntities(options);
			}
			
			@Override
			public Future<Integer> later()
			{
				throw new UnsupportedOperationException("Not implemented yet. Depends on async count");
			}
		};
		
	}

	@Override
	public CommandTerminator<T> returnUnique()
	{
		fetchFirst(1);
		return new CommandTerminator<T>()
		{
			@Override
			public T now()
			{
				return uniqueOrNull(StandardRootFindCommand.this.execute());
			}

			private T uniqueOrNull(Iterator<T> iterator)
			{
				if (iterator.hasNext())
				{
					T result = iterator.next();
					if (iterator.hasNext())
					{
						T extra = iterator.next();
						throw new IllegalStateException("Found more than one result " + extra);
					}
					return result;
				}
				else
				{
					return null;
				}
			}

			@Override
			public Future<T> later()
			{
				Future<QueryResultIterator<T>> future = StandardRootFindCommand.this.later();
				return new FutureAdaptor<QueryResultIterator<T>, T>(future)
				{
					@Override
					protected T adapt(QueryResultIterator<T> source)
					{
						return uniqueOrNull(source);
					}
				};
			}
		};
	}
	
	@Override
	public CommandTerminator<List<T>> returnAll()
	{
//		// get all in a single datastore call
//		if (options != null && options.getLimit() != null)
//		{
//			fetchFirst(options.getLimit());
//			fetchNextBy(options.getLimit());
//		}
//		else
//		{
			fetchFirst(Integer.MAX_VALUE);
			fetchNextBy(Integer.MAX_VALUE);
//		}
		
		return new CommandTerminator<List<T>>()
		{
			@Override
			public List<T> now()
			{
				return Lists.newArrayList(StandardRootFindCommand.this.execute());
			}

			@Override
			public Future<List<T>> later()
			{
				Future<QueryResultIterator<T>> future = StandardRootFindCommand.this.later();
				return new FutureAdaptor<QueryResultIterator<T>, List<T>>(future)
				{
					@Override
					protected List<T> adapt(QueryResultIterator<T> source)
					{
						List<T> result = new ArrayList<T>();
						while (source.hasNext())
						{
							result.add(source.next());
						}
						return result;
					}
				};
			}
		};
	}
	
	@Override
	public <P> CommandTerminator<Iterator<P>> returnParents()
	{
		return new CommandTerminator<Iterator<P>>()
		{
			@Override
			public Iterator<P> now()
			{
				return StandardRootFindCommand.this.<P>parentsCommandNow().now();
			}

			@Override
			public Future<Iterator<P>> later()
			{
				return StandardRootFindCommand.this.<P>parentsCommandNow().later();
			}
		};
	}
	
	@Override
	public <P> CommandTerminator<ParentsCommand<P>> returnParentsCommand()
	{
		return new CommandTerminator<ParentsCommand<P>>()
		{
			@Override
			public ParentsCommand<P> now()
			{
				return parentsCommandNow();
			}

			@Override
			public Future<ParentsCommand<P>> later()
			{
				return new ImmediateFuture<ParentsCommand<P>>(now());
			}
		};
	}
	public <P> ParentsCommand<P> parentsCommandNow()
	{
		Collection<Query> queries = queries();
		if (queries.size() == 1)
		{
			QueryResultIterator<Entity> childEntities = nowSingleQueryEntities(queries.iterator().next());
			return new StandardSingleParentsCommand<P>(this, childEntities, datastore.defaultActivationDepth);
		}
		else
		{
			List<Iterator<Entity>> iterators = new ArrayList<Iterator<Entity>>(queries.size());
			
			List<SortPredicate> sortPredicates = null;
			for (Query query : queries)
			{
				Iterator<Entity> entities = nowSingleQueryEntities(query);
				sortPredicates = query.getSortPredicates();
				iterators.add(entities);
			}
			return new StandardMultipleParentsCommand<P>(this, iterators, sortPredicates, datastore.defaultActivationDepth);
		}
	}

	public <P> Future<ParentsCommand<P>> parentsCommandLater()
	{
		return new ImmediateFuture<ParentsCommand<P>>(this.<P>parentsCommandNow());
	}
	
	@Override
	public QueryResultIterator<T> now()
	{
		if (getSettings().getCacheMode() == CacheMode.ON)
		{
			throw new IllegalStateException("Cannot cache results with iterator");
		}
		
		return execute();
	}
	
	protected QueryResultIterator<T> execute()
	{
		if (children == null)
		{
			Collection<Query> queries = getValidatedQueries();
			if (queries.size() > 1)
			{
				throw new IllegalStateException("Too many queries");
			}
			Query query = queries.iterator().next();

			QueryResultIterator<Entity> entities = nowSingleQueryEntities(query);

			Iterator<Entity> iterator = applyEntityFilter(entities);

			Iterator<T> instances = entitiesToInstances(iterator, propertyRestriction);
			return new BasicQueryResultIterator<T>(instances, entities);
		}
		else
		{
			try
			{
				Collection<Query> queries = getValidatedQueries();
				Iterator<Entity> entities = nowMultipleQueryEntities(queries);
				Iterator<T> result = entitiesToInstances(entities, propertyRestriction);
				return new NoCursorQueryResultIterator<T>(result);
			}
			catch (Exception e)
			{
				// only unchecked exceptions thrown from datastore service
				throw (RuntimeException) e.getCause();
			}
		}
	}

	@Override
	protected Query newQuery()
	{
		Query query = new Query(datastore.getConfiguration().typeToKind(type));
		applyFilters(query);
		if (sorts != null)
		{
			for (Sort sort : sorts)
			{
				query.addSort(sort.field, sort.direction);
			}
		}
		if (ancestor != null)
		{
			Key key = datastore.associatedKey(ancestor);
			if (key == null)
			{
				throw new IllegalArgumentException("Ancestor was not associated");
			}
			query.setAncestor(key);
		}
		
		// do not even get data for this instance if unactivated
		if (isUnactivated())
		{
			query.setKeysOnly();
		}
		
		return query;
	}

	public FetchOptions getFetchOptions()
	{
		return options;
	}

	/**
	 * Takes a normal instance Iterator<V> and makes a QueryResultIterator<V> using a
	 * low-level QueryResultIterator<Entity> to get the cursor.  
	 */
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

		@Override
		public List<Index> getIndexList()
		{
			throw new UnsupportedOperationException();
		}
	}
}
