package com.vercer.engine.persist.standard;

import com.google.appengine.api.datastore.Key;

class StandardUntypedSingleLoadCommand extends StandardDecodeCommand
{
	private final Key key;

	StandardUntypedSingleLoadCommand(StrategyObjectDatastore datastore, Key key)
	{
		super(datastore);
		this.key = key;
	}
	
	public <T> T returnResultNow()
	{
		return keyToInstance(key, null);
	}
}
