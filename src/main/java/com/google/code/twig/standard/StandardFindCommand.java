package com.google.code.twig.standard;

import java.util.Iterator;

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
		return new StandardRootFindCommand<T>(type, datastore, datastore.defaultActivationDepth);
	}
	
	@Override
	public StandardDescendantsCommand descendants(Object ancestor)
	{
		return new StandardDescendantsCommand(ancestor, datastore, datastore.defaultActivationDepth);
	}
}
