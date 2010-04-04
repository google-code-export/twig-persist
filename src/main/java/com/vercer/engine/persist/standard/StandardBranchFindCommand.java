package com.vercer.engine.persist.standard;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.vercer.engine.persist.FindCommand.BranchFindCommand;

final class StandardBranchFindCommand<T> extends StandardTypedFindCommand<T, BranchFindCommand<T>> implements BranchFindCommand<T>
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

	@Override
	protected Query newQuery()
	{
		Query query = parent.newQuery();
		applyFilters(query);
		return query;
	}
}
