package com.vercer.engine.persist.standard;

import java.util.Iterator;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.vercer.engine.persist.FindCommand.BranchFindCommand;

final class StandardBranchFindCommand<T> extends StandardTypedFindCommand<T, BranchFindCommand<T>>
	implements BranchFindCommand<T>
{
	private final StandardTypedFindCommand<T, ?> parent;

	StandardBranchFindCommand(StandardTypedFindCommand<T, ?> parent)
	{
		super(parent.datastore);
		this.parent = parent;
	}

	@Override
	protected FetchOptions getFetchOptions()
	{
		return parent.getFetchOptions();
	}

	public Future<Iterator<T>> returnResultsLater()
	{
		return futureMultiQueryInstanceIterator();
	}

	@SuppressWarnings("unchecked")
	public Iterator<T> returnResultsNow()
	{
		if (forceMultipleNow)
		{
			return nowMultiQueryInstanceIterator();
		}
		else
		{
			try
			{
				return (Iterator<T>) futureMultiQueryInstanceIterator().get();
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	@Override
	protected Query newQuery()
	{
		Query query = parent.newQuery();
		applyFilters(query);
		return query;
	}

}
