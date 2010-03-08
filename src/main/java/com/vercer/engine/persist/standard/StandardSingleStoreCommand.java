package com.vercer.engine.persist.standard;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.utils.FutureWrapper;
import com.google.common.collect.Iterables;
import com.vercer.engine.persist.StoreCommand.SingleStoreCommand;

final class StandardSingleStoreCommand<T> extends StandardBaseStoreCommand<T, SingleStoreCommand<T>> implements SingleStoreCommand<T>
{
	public StandardSingleStoreCommand(StandardStoreCommand command, T instance)
	{
		super(command);
		instances = Collections.singletonList(instance);
	}

	public Future<Key> returnKeyLater()
	{
		Future<Map<T, Key>> resultsLater = storeResultsLater();
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

	public Key returnKeyNow()
	{
		T instance = Iterables.getOnlyElement(instances);

		// cannot just call store because we may need to check the key
		Key parentKey = null;
		if (parent != null)
		{
			parentKey = command.datastore.associatedKey(parent);
		}
		Entity entity = command.datastore.instanceToEntity(instance, parentKey, null);

		if (unique)
		{
			checkUniqueKeys(Collections.singleton(entity));
		}

		return command.datastore.storeEntity(entity);
	}

}
