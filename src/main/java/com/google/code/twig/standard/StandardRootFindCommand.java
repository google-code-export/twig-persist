package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.utils.FutureWrapper;
import com.google.code.twig.FindCommand.ParentsCommand;
import com.google.code.twig.FindCommand.RootFindCommand;
import com.google.code.twig.CommandTerminator;
import com.google.code.twig.util.FutureAdaptor;
import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.Lists;

final class StandardRootFindCommand<T> extends StandardCommonFindCommand<RootFindCommand<T>>
		implements RootFindCommand<T>
{
	private final Class<?> type;
	private FetchOptions options;
	private Object ancestor;
	List<Sort> sorts;
	private boolean keysOnly;

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

	StandardRootFindCommand(Class<?> type, TranslatorObjectDatastore datastore)
	{
		super(datastore);
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
	public RootFindCommand<T> ancestor(Object ancestor)
	{
		this.ancestor = ancestor;
		return this;
	}

	@Override
	public RootFindCommand<T> unactivated()
	{
		keysOnly = true;
		return this;
	}

	@Override
	public RootFindCommand<T> addSort(String field)
	{
		return addSort(field, SortDirection.ASCENDING);
	}

	@Override
	public RootFindCommand<T> addSort(String field, SortDirection direction)
	{
		if (this.sorts == null)
		{
			this.sorts = new ArrayList<Sort>(2);
		}
		this.sorts.add(new Sort(field, direction));
		return this;
	}

	@Override
	public RootFindCommand<T> continueFrom(Cursor cursor)
	{
		if (this.options == null)
		{
			this.options = FetchOptions.Builder.withDefaults();
		}
		this.options.startCursor(cursor);
		return this;
	}

	@Override
	public RootFindCommand<T> finishAt(Cursor cursor)
	{
		if (this.options == null)
		{
			this.options = FetchOptions.Builder.withDefaults();
		}
		this.options.endCursor(cursor);
		return this;
	}

	@Override
	public RootFindCommand<T> fetchNextBy(int size)
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
	public RootFindCommand<T> fetchFirst(int size)
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
	public RootFindCommand<T> startFrom(int offset)
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
	public RootFindCommand<T> fetchMaximum(int limit)
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
			Collection<Query> queries = getValidatedQueries();
			if (queries.size() > 1)
			{
				throw new IllegalStateException("Multiple queries defined");
			}

			final Query query = queries.iterator().next();
			final Future<QueryResultIterator<Entity>> futureEntities = futureSingleQueryEntities(query);

			return new Future<QueryResultIterator<T>>()
			{
				private QueryResultIterator<T> doGet(QueryResultIterator<Entity> entities)
				{
						Iterator<Entity> iterator = applyEntityFilter(entities);
						Iterator<T> instances = entitiesToInstances(iterator, propertyRestriction);
						return new BasicQueryResultIterator<T>(instances, entities);
				}

				public QueryResultIterator<T> get() throws InterruptedException,
						ExecutionException
				{
						return doGet(futureEntities.get());
				}

				public QueryResultIterator<T> get(long timeout, TimeUnit unit)
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
				PreparedQuery prepared = datastore.servicePrepare(query);
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
				return uniqueOrNull(StandardRootFindCommand.this.now());
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
		// get all in a single datastore call
		if (options != null && options.getLimit() != null)
		{
			fetchFirst(options.getLimit());
		}
		else
		{
			fetchFirst(Integer.MAX_VALUE);
		}
		
		return new CommandTerminator<List<T>>()
		{
			@Override
			public List<T> now()
			{
				return Lists.newArrayList(StandardRootFindCommand.this.now());
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
				@SuppressWarnings("unchecked")
				Future<Iterator<Entity>> futureEntityIterator = (Future<Iterator<Entity>>) futureEntityIterator();
				return new FutureAdaptor<Iterator<Entity>, ParentsCommand<P>>(futureEntityIterator)
				{
					@Override
					protected ParentsCommand<P> adapt(Iterator<Entity> source)
					{
						return new StandardSingleParentsCommand<P>(StandardRootFindCommand.this, source);
					}
				};
			}
		};
	}
	public <P> ParentsCommand<P> parentsCommandNow()
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
				List<Future<QueryResultIterator<Entity>>> futures = multiQueriesToFutureEntityIterators(queries);
				for (Future<QueryResultIterator<Entity>> future : futures)
				{
					childIterators.add(future.get());
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
					return new StandardMultipleParentsCommand<P>(this, childIterators, sorts);
				}
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	@SuppressWarnings("unchecked")
	public <P> Future<ParentsCommand<P>> parentsCommandLater()
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
				return new StandardSingleParentsCommand<P>(StandardRootFindCommand.this, childEntities);
			}
		};
	}
	
	@Override
	public QueryResultIterator<T> now()
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
				// actually fetch the multiple queries in parallel
				final Iterator<T> result = this.<T> futureMultiQueryInstanceIterator().get();
				return new QueryResultIterator<T>()
				{
					@Override
					public Cursor getCursor()
					{
						throw new IllegalStateException("Cannot use cursor with merged queries");
					}

					@Override
					public boolean hasNext()
					{
						return result.hasNext();
					}

					@Override
					public T next()
					{
						return result.next();
					}

					@Override
					public void remove()
					{
						result.remove();
					}
				};
			}
			catch (Exception e)
			{
				if (e instanceof RuntimeException)
				{
					throw (RuntimeException) e;
				}
				else
				{
					throw (RuntimeException) e.getCause();
				}
			}
		}
	}

	@Override
	protected Query newQuery()
	{
		if (this.ancestor == null && this.datastore.getTransaction() != null)
		{
			throw new IllegalStateException(
					"Find command must have an ancestor in a transaction");
		}

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
				throw new IllegalArgumentException("Ancestor must be loaded in same session");
			}
			query.setAncestor(key);
		}
		if (keysOnly)
		{
			query.setKeysOnly();
		}
		return query;
	}

	public FetchOptions getFetchOptions()
	{
		return options;
	}

	public boolean isKeysOnly()
	{
		return keysOnly;
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
	}
}
