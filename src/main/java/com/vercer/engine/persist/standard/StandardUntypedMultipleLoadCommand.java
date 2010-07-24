package com.vercer.engine.persist.standard;

import java.util.Collection;
import java.util.Map;

import com.google.appengine.api.datastore.Key;

class StandardUntypedMultipleLoadCommand extends StandardDecodeCommand
{
	private final Collection<Key> keys;

	StandardUntypedMultipleLoadCommand(StrategyObjectDatastore datastore, Collection<Key> keys)
	{
		super(datastore);
		this.keys = keys;
	}

	public Map<Key, Object> returnResultsNow()
	{
		return keysToInstances(keys, null);
	}

}
