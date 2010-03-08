package com.vercer.engine.persist.standard;

import com.vercer.engine.persist.FindCommand;

public class StandardFindCommand extends StandardCommand implements FindCommand
{
	public StandardFindCommand(StrategyObjectDatastore datastore)
	{
		super(datastore);
	}

	public <T> RootFindCommand<T> type(Class<T> type)
	{
		return new StandardRootFindCommand<T>(type, datastore);
	}
}
