package com.google.code.twig.standard;

import com.google.appengine.api.datastore.Query;
import com.google.code.twig.FindCommand.ChildFindCommand;

final class StandardBranchFindCommand extends StandardCommonFindCommand<StandardBranchFindCommand> implements ChildFindCommand
{
	private final StandardCommonFindCommand<?> parent;

	StandardBranchFindCommand(StandardCommonFindCommand<?> parent)
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
	public StandardRootFindCommand<?> getRootCommand()
	{
		return parent.getRootCommand();
	}
}
