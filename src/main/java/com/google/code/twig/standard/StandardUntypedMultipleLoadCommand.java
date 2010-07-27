package com.google.code.twig.standard;

import java.util.Collection;
import java.util.Map;

import com.google.appengine.api.datastore.Key;

public class StandardUntypedMultipleLoadCommand extends StandardDecodeCommand
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
