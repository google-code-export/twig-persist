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
import java.util.Map.Entry;
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
import com.google.code.twig.annotation.Unique;
import com.google.code.twig.annotation.Version;
import com.google.code.twig.util.Pair;
import com.google.code.twig.util.reference.ObjectReference;
import com.google.code.twig.util.reference.SimpleObjectReference;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import com.google.common.primitives.Primitives;

abstract class StandardCommonStoreCommand<T, C extends StandardCommonStoreCommand<T, C>> extends StandardEncodeCommand implements CommonStoreCommand<T, C>
{
	final StandardStoreCommand command;
	Collection<? extends T> instances;
	List<?> ids;
	Key parentKey;
	boolean unique;
	
	// a set of instances already updated during a cascading update
	Set<Object> cascaded;
	Date date;

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

	final Transaction unique(Collection<Entity> entities)
	{
		Transaction txn = null;
		if (entities != null)
		{
			List<Key> keys = new ArrayList<Key>(entities.size());
			for (Entity entity : entities)
			{
				keys.add(entity.getKey());
			}
			
			txn = datastore.beginOrJoinTransaction();
			Map<Key, Entity> existing = datastore.serviceGet(keys, datastore.getDefaultSettings());
			if (!existing.isEmpty())
			{
				throw new IllegalStateException("Keys already exist: " + existing.keySet());
			}
		}
		return txn;
	}

	public static void updateInstanceState(Object instance, Key key, TranslatorObjectDatastore datastore)
	{
		try
		{
			Field idField = datastore.idField(instance.getClass());
			if (idField != null)
			{
				// only set numeric ids because they are the only ones auto-generated
				Class<?> type = idField.getType();
				if (Number.class.isAssignableFrom(type) || Primitives.allPrimitiveTypes().contains(type))
				{
					// convert the long or String to the declared key type
					Object converted = datastore.getTypeConverter().convert(key.getId(), type);
					idField.set(instance, converted);
				}
				else
				{
					// TODO check that an id was already set
				}
			}
			
			Field keyField = datastore.keyField(instance.getClass());
			if (keyField != null)
			{
				// must be a gae key field
				if (keyField.getType() == Key.class)
				{
					keyField.set(instance, key);
				}
				else if (keyField.getType() == String.class)
				{
					keyField.set(instance, KeyFactory.keyToString(key));
				}
				else
				{
					throw new IllegalStateException("Cannot set key to field " + keyField);
				}
			}
			
			Version version = instance.getClass().getAnnotation(Version.class);
			if (version != null)
			{
				// the version property might be mapped to different field name on the field
				Pair<Field, String> fieldAndProperty = datastore.getFieldAndPropertyForPath(version.value(), instance.getClass());
				
				if (fieldAndProperty != null)
				{
					fieldAndProperty.getFirst().set(instance, datastore.version(instance));
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
		final Map<Object, Entity> entities = instancesToEntities();

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

	final Map<Object, Entity> instancesToEntities()
	{
		Map<Object, Entity> entities = new LinkedHashMap<Object, Entity>(instances.size());

		Collection<Entity> uniques = null;
		for (T instance : instances)
		{
			// TODO implement bulk version checking and bulk backup
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
			
			if (unique || instance.getClass().isAnnotationPresent(Unique.class))
			{
				if (uniques == null) uniques = new ArrayList<Entity>();
				
				uniques.add(entity);
			}
		}

		// try to read each of the keys to verify it does not already exist
		unique(uniques);

		// if we are batching this will contain all referenced entities
		return entities;
	}

	@SuppressWarnings("unchecked")
	protected Map<T, Key> createKeyMapAndUpdateKeyCache(Map<Object, Entity> entities, List<Key> keys)
	{
		// build a map of instance to key
		HashMap<Object, Key> result = new HashMap<Object, Key>(keys.size());
		Iterator<Object> instances = entities.keySet().iterator();
		Iterator<Key> keyator = keys.iterator();
		while (instances.hasNext())
		{
			Object instance = instances.next();
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
			updateInstanceState(instance, key, datastore);

			assert datastore.associating == false;
			
			// the key is now complete for this activated instance
			if (!command.update)
			{
				// store always starts with version 1 for activated
				datastore.keyCache.cache(key, instance, 1);
			}
		}
		return (Map<T, Key>) result;
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
				txn = version(ImmutableMap.of(instance, entity));
				
				// backup for an update
				if (update)
				{
					// one of these might start a transaction
					txn = backup(instance, entity.getKey(), txn);
				}
				else
				{
					if (unique || instance.getClass().isAnnotationPresent(Unique.class))
					{
						txn = unique(Collections.singleton(entity));
					}
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
				
				// set the same date for all entities in this command
				assert date != null;
				backup.setProperty("backedup", date);
				
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

	protected Transaction version(Map<Object, Entity> instanceToEntity)
	{
		Transaction txn = null;
		Set<Entry<Object, Entity>> instanceAndEntities = instanceToEntity.entrySet();
		for (Entry<Object, Entity> instanceAndEntity : instanceAndEntities)
		{
			Object instance = instanceAndEntity.getKey();
			Entity entity = instanceAndEntity.getValue();
			
			if (instance.getClass().isAnnotationPresent(Version.class))
			{
				// get current version
				long current = datastore.version(instance);
				
				// if version is positive it was checked in this session
				if (current < 0)
				{
					if (txn == null) 
					{
						txn = datastore.beginOrJoinTransaction();
					}
					
					try
					{
						// load existing entity to get current version
						Entity existing = datastore.serviceGet(entity.getKey(), datastore.getDefaultSettings());
						Long version = readEntityVersion(existing, instance.getClass());
						if (version == null)
						{
							// allow unversioned types to become versioned
							version = 1l;
						}
						
						// change to positive to indicate we checked it in this session
						current = -current;
						
						if (current != version)
						{
							throw new IllegalStateException("Versions not equal " + version + ":" + current);
						}
					}
					catch (EntityNotFoundException e)
					{
						txn.rollback();
						throw new IllegalArgumentException("Update missing entity " + entity.getKey());
					}
				}
				
				// increment the version locally
				datastore.keyCache.setVersion(instance, ++current);
				
				String name = instance.getClass().getAnnotation(Version.class).value();
				
				// add the version property to store with entity
				entity.setProperty(name, current);
			}
		}
		
		return txn;
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
		if (command.update || datastore.isAssociated(instance))
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

		// check we will not over write another entity
		if (!command.update && // only check when storing 
			cascaded == null && // we can over write when cascading
			!datastore.associating && // associate can return an existing instance
			datastore.associatedInstance(entity.getKey()) != null)
		{
			throw new IllegalStateException("Instance already associated with key " + entity.getKey());
		}
		
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
	
	@SuppressWarnings("unchecked")
	public C cascaded(Set<Object> cascaded)
	{
		this.cascaded = cascaded;
		return (C) this;
	}
	
	@SuppressWarnings("unchecked")
	public C date(Date date)
	{
		this.date = date;
		return (C) this;
	}
	
}
