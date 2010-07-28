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
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.utils.FutureWrapper;
import com.google.code.twig.FindCommand;
import com.google.code.twig.Terminator;
import com.google.code.twig.FindCommand.ParentsCommand;
import com.google.code.twig.FindCommand.RootFindCommand;
import com.google.code.twig.util.FutureAdaptor;
import com.google.code.twig.util.IteratorToListFunction;
import com.google.common.base.Function;
import com.google.common.collect.Lists;

final class StandardRootFindCommand<T> extends StandardTypedFindCommand<T, RootFindCommand<T>>
		implements RootFindCommand<T>
{
	private final Type type;
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

	StandardRootFindCommand(Type type, StrategyObjectDatastore datastore)
	{
		super(datastore);
		this.type = type;
	}

	@Override
	StandardRootFindCommand<T> getRootCommand()
	{
		return this;
	}

	@Override
	public RootFindCommand<T> ancestor(Object ancestor)
	{
		this.ancestor = ancestor;
		return this;
	}

	@Override
	public RootFindCommand<T> fetchNoFields()
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
	public RootFindCommand<T> maximumResults(int limit)
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
		return futureSingleQueryInstanceIterator();
	}

	@Override
	public int countResultsNow()
	{
		Collection<Query> queries = getValidatedQueries();
		if (queries.size() > 1)
		{
			throw new IllegalStateException("Too many queries");
		}

		Query query = queries.iterator().next();
		PreparedQuery prepared = this.datastore.servicePrepare(query);
		return prepared.countEntities();
	}

	@Override
	public Terminator<List<T>> fetchAll()
	{
		return new Terminator<List<T>>()
		{
			@Override
			public List<T> now()
			{
				fetchFirst(Integer.MAX_VALUE);
				return Lists.newArrayList(StandardRootFindCommand.this.now());
			}

			@Override
			public Future<List<T>> later()
			{
				fetchFirst(Integer.MAX_VALUE);
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
	public <P> Terminator<Iterator<P>> fetchParents()
	{
		return new Terminator<Iterator<P>>()
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
	public <P> Terminator<ParentsCommand<P>> fetchParentsCommand()
	{
		return new Terminator<ParentsCommand<P>>()
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
			return nowSingleQueryInstanceIterator();
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
					"You must set an ancestor if you run a find this in a transaction");
		}

		Query query = new Query(datastore.fieldStrategy.typeToKind(type));
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
}
