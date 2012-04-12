package com.google.code.twig.standard;

import java.util.Collection;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LoadCommand;

public class StandardLoadCommand extends StandardCommand implements LoadCommand
{
	protected StandardLoadCommand(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}

	@Override
	public <T> StandardTypedLoadCommand<T> type(Class<? extends T> type)
	{
		return new StandardTypedLoadCommand<T>(datastore, type);
	}

	public StandardUntypedSingleLoadCommand key(Key key)
	{
		return new StandardUntypedSingleLoadCommand(datastore, key, datastore.defaultActivationDepth);
	}
	
	public StandardUntypedMultipleLoadCommand keys(Collection<Key> keys)
	{
		return new StandardUntypedMultipleLoadCommand(datastore, keys, datastore.defaultActivationDepth);
	}
}
