package com.vercer.engine.persist.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreHelper;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.utils.FutureWrapper;
import com.google.common.collect.Maps;
import com.vercer.engine.persist.StoreCommand.CommonStoreCommand;

abstract class StandardBaseStoreCommand<T, C extends CommonStoreCommand<T, C>> implements CommonStoreCommand<T, C>
{
	final StandardStoreCommand command;
	Collection<T> instances;
	Object parent;
	boolean batch;
	boolean unique;

	public StandardBaseStoreCommand(StandardStoreCommand command)
	{
		this.command = command;
	}

	@SuppressWarnings("unchecked")
	public C parent(Object parent)
	{
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	public C batch()
	{
		batch = true;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	public C ensureUniqueKey()
	{
		unique = true;
		return (C) this;
	}

	void checkUniqueKeys(Collection<Entity> entities)
	{
		List<Key> keys = new ArrayList<Key>(entities.size());
		for (Entity entity : entities)
		{
			keys.add(entity.getKey());
		}
		Map<Key, Entity> map = command.datastore.serviceGet(keys);
		if (!map.isEmpty())
		{
			throw new IllegalStateException("Keys already exist: " + map);
		}
	}

	Future<Map<T, Key>> storeResultsLater()
	{
		Transaction transaction = command.datastore.getTransaction();
		final Map<T, Entity> entities = command.datastore.instancesToEntities(instances, parent, batch);
		if (unique)
		{
			checkUniqueKeys(entities.values());
		}
		final Future<List<Key>> put = AsyncDatastoreHelper.put(transaction, entities.values());

		return new FutureWrapper<List<Key>, Map<T,Key>>(put)
		{
			@Override
			protected Throwable convertException(Throwable t)
			{
				return t;
			}

			@Override
			protected Map<T, Key> wrap(List<Key> list) throws Exception
			{
				LinkedHashMap<T, Key> result = Maps.newLinkedHashMap();
				Iterator<T> instances = entities.keySet().iterator();
				Iterator<Key> keys = list.iterator();
				while (instances.hasNext())
				{
					result.put(instances.next(), keys.next());
				}
				return result;
			}
		};
	}
}
