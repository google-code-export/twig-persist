package com.vercer.engine.persist.standard;

import java.util.Collection;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.LoadCommand;

public class StandardLoadCommand extends StandardCommand implements LoadCommand
{
	StandardLoadCommand(StrategyObjectDatastore datastore)
	{
		super(datastore);
	}

	@Override
	public <T> StandardTypedLoadCommand<T> type(Class<T> type)
	{
		return new StandardTypedLoadCommand<T>(datastore, type);
	}

	public StandardUntypedSingleLoadCommand key(Key key)
	{
		return new StandardUntypedSingleLoadCommand(datastore, key);
	}
	
	public StandardUntypedMultipleLoadCommand keys(Collection<Key> keys)
	{
		return new StandardUntypedMultipleLoadCommand(datastore, keys);
	}
}
