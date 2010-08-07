package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreHelper;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.utils.FutureWrapper;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.StoreCommand.CommonStoreCommand;

abstract class StandardCommonStoreCommand<T, C extends StandardCommonStoreCommand<T, C>> extends StandardEncodeCommand implements CommonStoreCommand<T, C>
{
	final StandardStoreCommand command;
	Collection<? extends T> instances;
	List<?> ids;
	Key parentKey;
	boolean batch;
	boolean unique;

	StandardCommonStoreCommand(StandardStoreCommand command)
	{
		super(command.datastore);
		this.command = command;
	}

	@SuppressWarnings("unchecked")
	public final C parent(Object parent)
	{
		parentKey = datastore.associatedKey(parent);
		if (parentKey == null)
		{
			throw new IllegalArgumentException("Parent is not associated: " + parent);
		}
		return (C) this;
	}
	
	@SuppressWarnings("unchecked")
	final C parentKey(Key parentKey)
	{
		this.parentKey = parentKey;
		return (C) this;
	}


	@SuppressWarnings("unchecked")
	public final C batch()
	{
		batch = true;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	public final C ensureUniqueKey()
	{
		unique = true;
		return (C) this;
	}

	final void checkUniqueKeys(Collection<Entity> entities)
	{
		List<Key> keys = new ArrayList<Key>(entities.size());
		for (Entity entity : entities)
		{
			keys.add(entity.getKey());
		}
		Map<Key, Entity> map = datastore.serviceGet(keys);
		if (!map.isEmpty())
		{
			throw new IllegalStateException("Keys already exist: " + map);
		}
	}

	protected void setInstanceId(Object instance, Key key)
	{
		Field field = datastore.keyField(instance.getClass());
		try
		{
			// if there is a key field
			if (field != null)
			{
				// see if its current value is null or 0
				Object current = field.get(instance);
				if (current == null || current instanceof Number && ((Number) current).longValue() == 0)
				{
					Class<?> type = field.getType();
					Object idOrName = key.getId();
					
					// the key name could have been set explicitly when storing 
					if (idOrName == null)
					{
						idOrName = key.getName();
					}
					
					// convert the long or String to the declared key type
					Object converted = datastore.converter.convert(idOrName, type);
					field.set(instance, converted);
				}
			}
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	// TODO this should be in the multiple command but is here as shortcut for single command
	final Future<Map<T,Key>> storeInstancesLater()
	{
		// convert to entities ready to store
		final Map<T, Entity> entities = instancesToEntities();
		
		Transaction transaction = datastore.getTransaction();
		
		// actually put the entities in the datastore without blocking
		final Future<List<Key>> put = AsyncDatastoreHelper.put(transaction, entities.values());
	
		return new FutureWrapper<List<Key>, Map<T,Key>>(put)
		{
			@Override
			protected Throwable convertException(Throwable t)
			{
				return t;
			}
	
			@Override
			protected Map<T, Key> wrap(List<Key> keys) throws Exception
			{
				return createKeyMapAndUpdateCache(entities, keys);
			}
		};
	}
	
	@SuppressWarnings("unchecked")
	final Map<T, Entity> instancesToEntities()
	{
		Map<T, Entity> entities = new LinkedHashMap<T, Entity>(instances.size());
		if (batch)
		{
			// indicates that all entities should be collected for one put
			datastore.batched = (Map<Object, Entity>) entities;
		}

		if (unique)
		{
			// try to read each of the keys to verify it does not already exist
			checkUniqueKeys(entities.values());
		}

		for (T instance : instances)
		{
			// cannot define a key name
			Entity entity = instanceToEntity(instance, parentKey, null);
			entities.put(instance, entity);
		}
		
		if (batch)
		{
			// we are done collecting entities for this batch
			datastore.batched = null;
		}
		
		// if we are batching this will contain all referenced entities
		return entities;
	}

	protected Map<T, Key> createKeyMapAndUpdateCache(Map<T, Entity> entities, List<Key> keys)
	{
		// build a map of instance to key
		HashMap<T, Key> result = new HashMap<T, Key>(keys.size());
		Iterator<T> instances = entities.keySet().iterator();
		Iterator<Key> keyor = keys.iterator();
		while (instances.hasNext())
		{
			Key key = keyor.next();
			T instance = instances.next();
			result.put(instance, key);
			datastore.associate(instance, key);
			setInstanceId(instance, key);
		}
		return result;
	}

	protected Entity instanceToEntity(Object instance, Key parentKey, Object id)
	{
		String kind = datastore.getFieldStrategy().typeToKind(instance.getClass());
		
		// push a new encode context
		KeySpecification existingEncodeKeySpec = datastore.encodeKeySpec;
		datastore.encodeKeySpec = new KeySpecification(kind, parentKey, id);

		// if we are updating the key is already in the key cache
		if (!command.update)
		{
			// set incomplete key reference before stored for back references 
			datastore.keyCache.cacheKeyReferenceForInstance(instance, datastore.encodeKeySpec.toObjectReference());
		}
			
		// translate fields to properties - sets parent and id on key
		PropertyTranslator encoder = datastore.encoder(instance);
		Set<Property> properties = encoder.typesafeToProperties(instance, Path.EMPTY_PATH, datastore.indexed);
		if (properties == null)
		{
			throw new IllegalStateException("Could not translate instance: " + instance);
		}

		// the key will now be set with id and parent
		Entity entity = createEntity();

		// will trigger referenced instances to be stored
		transferProperties(entity, properties);
		
		// we can store all entities for a single batch put
		if (datastore.batched != null)
		{
			datastore.batched.put(instance, entity);
		}
		
		// pop the encode context
		datastore.encodeKeySpec = existingEncodeKeySpec;
		
		return entity;
	}
	
	/**
	 * Potentially store an entity in the datastore.
	 */
	protected Key entityToKey(Entity entity)
	{
		// we could be just pretending to store to process the instance to get its key
		if (datastore.associating || batch)
		{
			// do not save the entity because we just want the key
			Key key = entity.getKey();
			if (!key.isComplete())
			{
				// incomplete keys are no good to us
				throw new IllegalArgumentException("Associating entity does not have complete key: " + entity);
			}
			return key;
		}
		else
		{
			// actually put the entity in the datastore
			return datastore.servicePut(entity);
		}
	}

	protected List<Key> entitiesToKeys(Collection<Entity> entities)
	{
		// we could be just pretending to store to process the instance to get its key
		if (datastore.associating || batch)
		{
			// do not save the entity because we just want the keys
			List<Key> keys = new ArrayList<Key>(entities.size());
			for (Entity entity : entities)
			{
				Key key = entity.getKey();
				
				if (!key.isComplete())
				{
					// incomplete keys are no good to us
					throw new IllegalArgumentException("Associating entity does not have complete key: " + entity);
				}
				keys.add(key);
			}
			
			return keys;
		}
		else
		{
			// actually put the entity in the datastore
			return datastore.servicePut(entities);
		}
	}
}