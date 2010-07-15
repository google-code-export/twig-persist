package com.vercer.engine.persist.standard;

import com.google.appengine.api.datastore.Query;
import com.vercer.engine.persist.FindCommand.ChildFindCommand;

final class StandardBranchFindCommand<T> extends StandardTypedFindCommand<T, ChildFindCommand<T>> implements ChildFindCommand<T>
{
	private final StandardTypedFindCommand<T, ?> parent;

	StandardBranchFindCommand(StandardTypedFindCommand<T, ?> parent)
	{
		super(parent.datastore);
		this.parent = parent;
	}

	@Override
	protected Query newQuery()
	{
		Query query = parent.newQuery();
		applyFilters(query);
		return query;
	}
	
	@Override
	public StandardRootFindCommand<T> getRootCommand()
	{
		return parent.getRootCommand();
	}
}
