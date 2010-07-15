package com.vercer.engine.persist.standard;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.common.collect.Maps;
import com.vercer.engine.persist.StoreCommand.MultipleStoreCommand;
import com.vercer.util.LazyProxy;

public class StandardMultipleStoreCommand<T> extends StandardBaseStoreCommand<T, MultipleStoreCommand<T>> implements MultipleStoreCommand<T>
{
	public StandardMultipleStoreCommand(StandardStoreCommand command, Collection<T> instances)
	{
		super(command);
		this.instances = instances;
	}

	public Future<Map<T, Key>> returnKeysLater()
	{
		return storeResultsLater();
	}

	public Map<T, Key> returnKeysNow()
	{
		final Map<T, Entity> entities = command.datastore.instancesToEntities(instances, parent, batch);

		if (unique)
		{
			checkUniqueKeys(entities.values());
		}

		final List<Key> put = command.datastore.entitiesToKeys(entities.values());

		// use a lazy map because often keys ignored
		return new LazyProxy<Map<T, Key>>(Map.class)
		{
			@Override
			protected Map<T, Key> newInstance()
			{
				LinkedHashMap<T, Key> result = Maps.newLinkedHashMap();
				Iterator<T> instances = entities.keySet().iterator();
				Iterator<Key> keys = put.iterator();
				while (instances.hasNext())
				{
					result.put(instances.next(), keys.next());
				}
				return result;
			}
		}.newProxy();
	}
}
