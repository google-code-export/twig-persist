package com.vercer.engine.persist.standard;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.StoreCommand.MultipleStoreCommand;

class StandardMultipleStoreCommand<T> extends StandardCommonStoreCommand<T, StandardMultipleStoreCommand<T>> implements MultipleStoreCommand<T, StandardMultipleStoreCommand<T>>
{
	StandardMultipleStoreCommand(StandardStoreCommand command, Collection<T> instances)
	{
		super(command);
		this.instances = instances;
	}

	public Future<Map<T, Key>> returnKeysLater()
	{
		return storeInstancesLater();
	}

	public Map<T, Key> returnKeysNow()
	{
		// convert into entities ready to store
		Map<T, Entity> entities = instancesToEntities();

		// actually put the entities in the datastore
		List<Key> keys = entitiesToKeys(entities.values());
		
		// make a map to return
		return createKeyMapAndUpdateCache(entities, keys);
	}

	@Override
	public StandardMultipleStoreCommand<T> ids(String... ids)
	{
		return ids(Arrays.asList(ids));
	}

	@Override
	public StandardMultipleStoreCommand<T> ids(Long... ids)
	{
		return ids(Arrays.asList(ids));
	}

	@Override
	public StandardMultipleStoreCommand<T> ids(List<?> ids)
	{
		this.ids = ids;
		return this;
	}
}
