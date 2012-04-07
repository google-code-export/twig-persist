package com.google.code.twig.standard;

import com.google.code.twig.FindCommand;

public class StandardFindCommand extends StandardCommand implements FindCommand
{
	protected StandardFindCommand(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}

	@Override
	public <T> StandardRootFindCommand<T> type(Class<? extends T> type)
	{
		return new StandardRootFindCommand<T>(type, datastore);
	}
}
