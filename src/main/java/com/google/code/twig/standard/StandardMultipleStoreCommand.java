package com.google.code.twig.standard;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.code.twig.StoreCommand.MultipleStoreCommand;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

public class StandardMultipleStoreCommand<T> extends StandardCommonStoreCommand<T, StandardMultipleStoreCommand<T>> implements MultipleStoreCommand<T, StandardMultipleStoreCommand<T>>
{
	StandardMultipleStoreCommand(StandardStoreCommand command, Collection<? extends T> instances)
	{
		super(command);
		this.instances = instances;

		// we check that instances are associated deeper in the command
	}

	public Future<Map<T, Key>> later()
	{
		return storeInstancesLater();
	}

	public Map<T, Key> now()
	{
		if (instances.isEmpty()) return Collections.emptyMap();
		
		// convert into entities ready to store
		Map<Object, Entity> instanceToEntity = (Map<Object, Entity>) instancesToEntities();

		// we can get null entities when they are already stored by being referenced
		Map<Object, Entity> filteredInstanceToEntity = Maps.filterValues(instanceToEntity,  Predicates.notNull());
		
		if (datastore.associating)
		{
			throw new IllegalStateException("Only single store is supported for associate");
		}
			
		Transaction txn = null;
		try
		{
			txn = version(filteredInstanceToEntity);
			
			// TODO allow command to override settings
			List<Key> keys = datastore.servicePut(filteredInstanceToEntity.values(), datastore.getDefaultSettings());
			
			if (txn != null)
			{
				txn.commit();
			}
			
			// TODO multiple backup 
			
			return createKeyMapAndUpdateKeyCache(filteredInstanceToEntity, keys);
		}
		catch (RuntimeException e)
		{
			if (txn != null && txn.isActive())
			{
				txn.rollback();
			}
			throw e;
		}
		// make a map to return
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
