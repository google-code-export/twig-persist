package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.AsyncDatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.utils.FutureWrapper;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.StoreCommand.CommonStoreCommand;
import com.google.code.twig.annotation.Backup;
import com.google.code.twig.annotation.Version;
import com.google.code.twig.util.reference.ObjectReference;
import com.google.code.twig.util.reference.SimpleObjectReference;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.primitives.Primitives;

abstract class StandardCommonStoreCommand<T, C extends StandardCommonStoreCommand<T, C>> extends StandardEncodeCommand implements CommonStoreCommand<T, C>
{
	final StandardStoreCommand command;
	Collection<? extends T> instances;
	List<?> ids;
	Key parentKey;
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
	public final C ensureUniqueKey()
	{
		unique = true;
		return (C) this;
	}

	final Transaction checkUniqueKeys(Collection<Entity> entities)
	{
		List<Key> keys = new ArrayList<Key>(entities.size());
		for (Entity entity : entities)
		{
			keys.add(entity.getKey());
		}
		
		Transaction txn = datastore.beginOrJoinTransaction();
		Map<Key, Entity> map = datastore.serviceGet(keys, datastore.getDefaultSettings());
		if (!map.isEmpty())
		{
			throw new IllegalStateException("Keys already exist: " + map);
		}
		return txn;
	}

	public static void setInstanceId(Object instance, Key key, TranslatorObjectDatastore datastore)
	{
		Field field = datastore.idField(instance.getClass());
		try
		{
			// check that we have an id field
			if (field != null)
			{
				// only set numeric ids because they are the only ones auto-generated
				Class<?> type = field.getType();
				if (Number.class.isAssignableFrom(type) || Primitives.allPrimitiveTypes().contains(type))
				{
					// convert the long or String to the declared key type
					Object converted = datastore.getTypeConverter().convert(key.getId(), type);
					field.set(instance, converted);
				}
				else
				{
					// TODO check that an id was already set
				}
			}
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public static void setInstanceKey(Object instance, Key key, TranslatorObjectDatastore datastore)
	{
		Field field = datastore.keyField(instance.getClass());
		try
		{
			// check that we have an id field
			if (field != null)
			{
				// must be a gae key field
				if (field.getType() == Key.class)
				{
					field.set(instance, key);
				}
				else if (field.getType() == String.class)
				{
					field.set(instance, KeyFactory.keyToString(key));
				}
				else
				{
					throw new IllegalStateException("Cannot set key to field " + field);
				}
			}
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalStateException(e);
		}
	}

	// TODO this should be in the multiple command but is here as shortcut for single command
	final Future<Map<T,Key>> storeInstancesLater()
	{
		// convert to entities ready to store
		final Map<T, Entity> entities = instancesToEntities();

		// we can get null entities when they are already stored
		Collection<Entity> filtered = Collections2.filter(entities.values(), Predicates.notNull());

		Transaction transaction = datastore.getTransaction();

		// actually put the entities in the datastore without blocking
		AsyncDatastoreService service = DatastoreServiceFactory.getAsyncDatastoreService();
		final Future<List<Key>> put = service.put(transaction, new ArrayList<Entity>(filtered));

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
				return createKeyMapAndUpdateKeyCache(entities, keys);
			}
		};
	}

	final Map<T, Entity> instancesToEntities()
	{
		Map<T, Entity> entities = new LinkedHashMap<T, Entity>(instances.size());

		if (unique)
		{
			// try to read each of the keys to verify it does not already exist
			checkUniqueKeys(entities.values());
		}

		for (T instance : instances)
		{
			// TODO implement bulk version checking and bulk backup
			if (instance.getClass().isAnnotationPresent(Version.class))
			{
				throw new UnsupportedOperationException("Versioning multiple instances not implemented");
			}
			if (instance.getClass().isAnnotationPresent(Backup.class))
			{
				throw new UnsupportedOperationException("Backup multiple instances not implemented");
			}
			
			// check instance was not stored while storing another instance
			Entity entity = null;
			
			// get if we update or we are doing a store and still don't have key
			if (command.update || datastore.associatedKey(instance) == null)
			{
				// cannot define a key name
				entity = instanceToEntity(instance, parentKey, null);
			}

			// put null if instance was already stored - don't store again
			entities.put(instance, entity);
		}

		// if we are batching this will contain all referenced entities
		return entities;
	}

	protected Map<T, Key> createKeyMapAndUpdateKeyCache(Map<T, Entity> entities, List<Key> keys)
	{
		// build a map of instance to key
		HashMap<T, Key> result = new HashMap<T, Key>(keys.size());
		Iterator<T> instances = entities.keySet().iterator();
		Iterator<Key> keyator = keys.iterator();
		while (instances.hasNext())
		{
			T instance = instances.next();
			Key key;

			// we may not have stored the entity in this put
			if (entities.get(instance) == null)
			{
				// referenced entity was stored in a previous put
				key = datastore.associatedKey(instance);
			}
			else
			{
				// entity was stored now so get key
				key = keyator.next();
			}

			assert key != null;

			result.put(instance, key);

			// set the ids if auto-generated
			setInstanceId(instance, key, datastore);
			setInstanceKey(instance, key, datastore);

			// the key is now complete for this activated instance
			assert datastore.associating == false;
			datastore.associate(instance, key, 1);
		}
		return result;
	}

	protected Key instanceToKey(Object instance, Object id)
	{
		// need to check before create entity as incomplete key gets associated
		boolean update = datastore.isAssociated(instance);
		
		// this will store any parents or related instances
		Entity entity = instanceToEntity(instance, parentKey, id);
		
		Key key;
		if (datastore.associating)
		{
			// just return key from entity with no datastore operation
			key = entity.getKey();
			if (!key.isComplete())
			{
				throw new IllegalStateException("Key was not complete: " + key);
			}
		}
		else
		{
			Transaction txn = null;
			try
			{
				// only version and backup for an update
				if (update)
				{
					// one of these might start a transaction
					txn = version(instance, entity);
					txn = backup(instance, entity.getKey(), txn);
				}
				else if (unique)
				{
					txn = checkUniqueKeys(Collections.singleton(entity));
				}
				
				// TODO allow command to override settings
				key = datastore.servicePut(entity, datastore.getDefaultSettings());
				
				if (txn != null)
				{
					txn.commit();
				}
			}
			catch (RuntimeException e)
			{
				if (txn != null && txn.isActive())
				{
					txn.rollback();
				}
				throw e;
			}
		}

		return key;
	}

	private Transaction backup(Object instance, Key key, Transaction txn)
	{
		if (instance.getClass().isAnnotationPresent(Backup.class))
		{
			if (txn == null)
			{
				txn = datastore.beginOrJoinTransaction();
			}
			
			try
			{
				Entity existing = datastore.serviceGet(key, datastore.getDefaultSettings());
				
				Entity backup = new Entity("backup", key);
				backup.setPropertiesFrom(existing);
				backup.setProperty("backedup", new Date());
				
				datastore.servicePut(backup, datastore.getDefaultSettings());
				
				return txn;
			}
			catch (EntityNotFoundException e)
			{
				txn.rollback();
				throw new IllegalStateException("Cannot find entity to backup " + key);
			}
		}
		return txn;
	}

	protected Transaction version(Object instance, Entity entity)
	{
		if (instance.getClass().isAnnotationPresent(Version.class))
		{
			Transaction txn = datastore.beginOrJoinTransaction();
			try
			{
				// get current version
				long local = datastore.version(instance);

				// if version is positive it was loaded in this session
				if (local < 0)
				{
					// change to positive to indicate we checked it in this session
					local = -local;
					
					// load existing entity to get current version
					Entity existing = datastore.serviceGet(entity.getKey(), datastore.getDefaultSettings());
					Long version = version(existing, instance.getClass());
					if (version == null)
					{
						// allow unversioned types to become versioned
						version = 1l;
					}

					if (local != version)
					{
						throw new IllegalStateException("Versions not equal " + version + ":" + local);
					}
				}
				
				// increment the version locally
				datastore.keyCache.setVersion(instance, ++local);

				String name = instance.getClass().getAnnotation(Version.class).value();
				
				// add the version property to store with entity
				entity.setProperty(name, local);
				
				return txn;
			}
			catch (EntityNotFoundException e)
			{
				txn.rollback();
				throw new IllegalArgumentException("Update missing entity " + entity.getKey());
			}
		}
		return null;
	}

	/**
	 * All instances sent to the datastore come through this method - both single and multiple puts
	 */
	protected Entity instanceToEntity(Object instance, Key parentKey, Object id)
	{
		String kind = datastore.getConfiguration().typeToKind(instance.getClass());

		// push a new encode context
		KeyDetails existingEncodeKeySpec = datastore.encodeKeyDetails;
		datastore.encodeKeyDetails = new KeyDetails(kind, parentKey, id);

		// if we are updating the key is already in the key cache
		if (command.update || command.cascade && datastore.isAssociated(instance))
		{
			// get the key associated with this instance
			Key associatedKey = datastore.associatedKey(instance);

			if (associatedKey == null)
			{
				throw new IllegalArgumentException("Cannot update unassociated instance " + instance + ". Use store instead");
			}
			
			// make sure we are updating an activated instance
			if (!datastore.isActivated(instance))
			{
				throw new IllegalStateException("Cannot update unactivated instance " + instance);
			}

			// set the id and ancestors to ensure entity will be overwritten
			if (associatedKey.getName() != null)
			{
				datastore.encodeKeyDetails.setName(associatedKey.getName());
			}
			else if (associatedKey.getId() != 0)
			{
				datastore.encodeKeyDetails.setId(associatedKey.getId());
			}
			else
			{
				throw new IllegalStateException("No id found for associated instance " + instance);
			}

			if (associatedKey.getParent() != null)
			{
				datastore.encodeKeyDetails.setParentKeyReference(new SimpleObjectReference<Key>(associatedKey.getParent()));
			}
		}
		else
		{
			// we are storing or associating a new instance
			if (datastore.isAssociated(instance))
			{
				throw new IllegalArgumentException("Cannot store associated instance " + instance + ". Use update instead.");
			}

			// set incomplete key reference before stored for back references
			datastore.keyCache.cacheKeyReferenceForInstance(instance, datastore.encodeKeyDetails.toKeyReference());

			// don't bother getting auto ids when just associating
			if (datastore.associating == false)
			{
				maybeSetAllocatedId(instance);
			}
		}

		// translate fields to properties - sets key ancestors and id
		PropertyTranslator encoder = datastore.encoder(instance);
		Set<Property> properties = encoder.encode(instance, Path.EMPTY_PATH, false);
		if (properties == null)
		{
			throw new IllegalStateException("Could not translate instance: " + instance);
		}

		// the key will now be set with id and ancestors
		Entity entity = createEntity();

		// will trigger referenced instances to be stored
		transferProperties(entity, properties);

		// pop the encode context
		datastore.encodeKeyDetails = existingEncodeKeySpec;

		// check that id field is numeric if auto-generating id
		Field idField = datastore.idField(instance.getClass());
		if (!entity.getKey().isComplete() 		// no id was set
				&& idField != null 				// there is an @id field
				&& !idField.getType().isPrimitive()	// its not a primitive
				&& !Number.class.isAssignableFrom(idField.getType())) // its not a number
		{
			throw new IllegalStateException("No id was set for " + instance + " and its @Id field is not numeric");
		}
		
		return entity;
	}

	private void maybeSetAllocatedId(Object instance)
	{
		Key parentKey;
		long allocateIdsForType = datastore.getConfiguration().allocateIdsFor(instance.getClass());
		if (allocateIdsForType > 0)
		{
			if (datastore.encodeKeyDetails.getId() == null)
			{
				parentKey = null;
				ObjectReference<Key> parentKeyReference = datastore.encodeKeyDetails.getParentKeyReference();
				StringBuilder kindAndParentBuilder = new StringBuilder();
				kindAndParentBuilder.append(datastore.encodeKeyDetails.getKind());
				if (parentKeyReference != null)
				{
					parentKey = parentKeyReference.get();
					kindAndParentBuilder.append(parentKey.toString());
				}

				String kindAndParent = kindAndParentBuilder.toString();
				Iterator<Key> range = null;
				if (datastore.allocatedIdRanges == null)
				{
					datastore.allocatedIdRanges = new HashMap<String, Iterator<Key>>();
				}
				else
				{
					range = datastore.allocatedIdRanges.get(kindAndParent);
				}

				if (range == null || range.hasNext() == false)
				{
					range = datastore.getDefaultService().allocateIds(
									parentKey,
									datastore.encodeKeyDetails.getKind(),
									allocateIdsForType)
									.iterator();

					datastore.allocatedIdRanges.put(kindAndParent, range);
				}

				// get the id from the key - the rest of the key spec should be the same
				datastore.encodeKeyDetails.setId(range.next().getId());
			}
		}
	}
}
