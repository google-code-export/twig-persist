package com.vercer.engine.persist.standard;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.vercer.engine.persist.FindCommand.RootFindCommand;

final class StandardRootFindCommand<T> extends StandardTypedFindCommand<T, RootFindCommand<T>> implements RootFindCommand<T>
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
	public RootFindCommand<T>  fetchFirst(int size)
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
	public Future<QueryResultIterator<T>> returnResultsLater()
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
	public QueryResultList<T> returnAllResultsNow()
	{
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	@Override
	public Future<QueryResultList<T>> returnAllResultsLater()
	{
		throw new UnsupportedOperationException("Not yet implemented");
	}
	
	@Override
	public QueryResultIterator<T> returnResultsNow()
	{
		if (children == null)
		{
			return nowSingleQueryInstanceIterator();
		}
		else
		{
			try
			{
				final Iterator<T> result = this.<T>futureMultiQueryInstanceIterator().get();
				return new QueryResultIterator<T>()
				{
					public Cursor getCursor()
					{
						throw new IllegalStateException("Cannot use cursor with merged queries");
					}

					public boolean hasNext()
					{
						return result.hasNext();
					}

					public T next()
					{
						return result.next();
					}

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
			throw new IllegalStateException("You must set an ancestor if you run a find this in a transaction");
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
