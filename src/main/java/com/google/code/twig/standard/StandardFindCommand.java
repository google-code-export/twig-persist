package com.google.code.twig.standard;

import com.google.code.twig.FindCommand;

class StandardFindCommand extends StandardCommand implements FindCommand
{
	StandardFindCommand(StrategyObjectDatastore datastore)
	{
		super(datastore);
	}

	@Override
	public <T> RootFindCommand<T> type(Class<T> type)
	{
		return new StandardRootFindCommand<T>(type, datastore);
	}
}
