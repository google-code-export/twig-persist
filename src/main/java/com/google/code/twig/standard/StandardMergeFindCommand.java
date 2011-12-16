package com.google.code.twig.standard;

import com.google.appengine.api.datastore.Query;
import com.google.code.twig.FindCommand.MergeFindCommand;

public class StandardMergeFindCommand extends StandardCommonFindCommand<MergeFindCommand> implements MergeFindCommand
{
	private final StandardCommonFindCommand<?> parent;

	StandardMergeFindCommand(StandardCommonFindCommand<?> parent)
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
