package com.google.code.twig.standard;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.utils.FutureWrapper;
import com.google.code.twig.StoreCommand.SingleStoreCommand;
import com.google.common.collect.Iterables;

final class StandardSingleStoreCommand<T> extends StandardCommonStoreCommand<T, StandardSingleStoreCommand<T>> implements SingleStoreCommand<T, StandardSingleStoreCommand<T>>
{
	StandardSingleStoreCommand(StandardStoreCommand command, T instance)
	{
		super(command);
		instances = Collections.singletonList(instance);
		if (!command.update && command.datastore.associatedKey(instance) != null)
		{
			throw new IllegalArgumentException("Cannot store associated instance. Use update instead.");
		}
		else if (command.update && command.datastore.associatedKey(instance) == null)
		{
			throw new IllegalArgumentException("Cannot update non-associated instance. Use store instead.");
		}
	}

	public Future<Key> later()
	{
		Future<Map<T, Key>> resultsLater = storeInstancesLater();
		return new FutureWrapper<Map<T, Key>, Key>(resultsLater)
		{
			@Override
			protected Throwable convertException(Throwable arg0)
			{
				return arg0;
			}

			@Override
			protected Key wrap(Map<T, Key> keys) throws Exception
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
		Entity entity = instanceToEntity(instance, parentKey, id);

		if (unique)
		{
			checkUniqueKeys(Collections.singleton(entity));
		}
		Key key = entityToKey(entity);
		
		datastore.associate(instance, key);
		setInstanceId(instance, key);
		
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
