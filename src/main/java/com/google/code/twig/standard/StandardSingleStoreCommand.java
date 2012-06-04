package com.google.code.twig.standard;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.StoreCommand.SingleStoreCommand;
import com.google.code.twig.util.FutureAdaptor;
import com.google.common.collect.Iterables;

public final class StandardSingleStoreCommand<T> extends StandardCommonStoreCommand<T, StandardSingleStoreCommand<T>> implements SingleStoreCommand<T, StandardSingleStoreCommand<T>>
{
	StandardSingleStoreCommand(StandardStoreCommand command, T instance)
	{
		super(command);
		instances = Collections.singletonList(instance);
	}

	public Future<Key> later()
	{
		Future<Map<T, Key>> resultsLater = storeInstancesLater();
		return new FutureAdaptor<Map<T, Key>, Key>(resultsLater)
		{
			@Override
			protected Key adapt(Map<T, Key> keys)
			{
				return Iterables.getOnlyElement(keys.values());
			}
		};
	}

	public Key now()
	{
		T instance = Iterables.getOnlyElement(instances);

		Object id = null;
		if (ids != null)
		{
			id = Iterables.getOnlyElement(ids);
		}
		
		Key key = instanceToKey(instance, id);

		setInstanceId(instance, key, datastore);
		setInstanceKey(instance, key, datastore);
		
		// when associating we might not activate the instance
		datastore.keyCache.cache(key, instance, 1);
		
		return key;
	}
	
	@Override
	public StandardSingleStoreCommand<T> id(long id)
	{
		this.ids = Collections.singletonList(id);
		return this;
	}

	@Override
	public StandardSingleStoreCommand<T> id(String id)
	{
		this.ids = Collections.singletonList(id);
		return this;
	}
}
