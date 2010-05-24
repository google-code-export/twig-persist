package com.vercer.engine.persist.standard;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.vercer.engine.persist.ObjectDatastore;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PropertyMapToSet;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.engine.util.Entities;
import com.vercer.util.reference.ObjectReference;

/**
 * Stateless base responsible for core datastore functionality that does not
 * depend on an instances identity. Encoding and decoding instances and entities
 * is performed by the PropertyTranslator.
 *
 * @author John Patterson <john@vercer.com>
 */
public abstract class AbstractStatelessObjectDatastore extends DatastoreServiceContainer implements ObjectDatastore
{
	private PropertyTranslator translator;
	private boolean indexed;

	protected AbstractStatelessObjectDatastore(DatastoreService service)
	{
		super(service);
	}

	protected final void setPropertyTranslator(PropertyTranslator translator)
	{
		this.translator = translator;
	}

	protected final void setIndexed(boolean indexed)
	{
		this.indexed = indexed;
	}

	protected final Key store(Object instance, Key parentKey, String name)
	{
		Entity entity = instanceToEntity(instance, parentKey, name);
		Key key = storeEntity(entity);

		return key;
	}

	protected final Entity instanceToEntity(Object instance, Key parentKey, String name)
	{
		onBeforeEncode(instance);
		Entity entity;
		try
		{
			Set<Property> properties = translator.typesafeToProperties(instance, Path.EMPTY_PATH, indexed);
			if (properties == null)
			{
				throw new IllegalStateException("Could not translate instance: " + instance);
			}

			String kind = typeToKind(instance.getClass());

			entity = createEntity(new KeySpecification(kind, parentKey, name));

			transferProperties(entity, properties);
		}
		catch (Exception e)
		{
			throw newExceptionOnTranslateWrite(e, instance);
		}
		onAfterEncode(instance, entity);
		return entity;
	}

	private void transferProperties(Entity entity, Collection<Property> properties)
	{
		for (Property property : properties)
		{
			// dereference object references
			Object value = property.getValue();
			value = dereferencePropertyValue(value);

			if (property.isIndexed())
			{
				entity.setProperty(property.getPath().toString(), value);
			}
			else
			{
				entity.setUnindexedProperty(property.getPath().toString(), value);
			}
		}
	}

	public static Object dereferencePropertyValue(Object value)
	{
		if (value instanceof ObjectReference<?>)
		{
			value = ((ObjectReference<?>)value).get();
		}
		else if (value instanceof List<?>)
		{
			// we know the value is a mutable list from ListTranslator
			@SuppressWarnings("unchecked")
			List<Object> values = (List<Object>) value;
			for (int i = 0; i < values.size(); i++)
			{
				Object item = values.get(i);
				if (item instanceof ObjectReference<?>)
				{
					// dereference the value and set it in-place
					Object dereferenced = ((ObjectReference<?>) item).get();
					values.set(i, dereferenced);  // replace the reference
				}
			}
		}
		return value;
	}

	/**
	 * Give subclasses a chance to intercept all datastore puts
	 */
	protected Key storeEntity(Entity entity)
	{
		return servicePut(entity);
	}

	public final Key store(Object instance, String name)
	{
		return store(instance, name, null);
	}

	protected final Key store(Object value, Key parentKey)
	{
		return store(value, parentKey, null);
	}

	public final Key store(Object instance)
	{
		return store(instance, (Key) null, null);
	}

	protected Entity createEntity(KeySpecification spec)
	{
		if (spec.isComplete())
		{
			return new Entity(spec.toKey());
		}
		else
		{
			ObjectReference<Key> parentKeyReference = spec.getParentKeyReference();
			Key parentKey = parentKeyReference == null ? null : parentKeyReference.get();
			return Entities.createEntity(spec.getKind(), null, parentKey);
		}
	}

	/**
	 * Override to provide shorter kind names reducing the key size
	 * @param clazz The class to store
	 * @return A shortened kind name
	 */
	protected String typeToKind(Type type)
	{
		Class<?> clazz = GenericTypeReflector.erase(type);
		return clazz.getName();
	}

	protected Type kindToType(String kind)
	{
		try
		{
			return Class.forName(kind);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public final <T> T load(Class<T> type, Object key)
	{
		return load(type, key, null);
	}

	protected final <T> T internalLoad(Class<T> type, Object converted, Key parent)
	{
		String kind = typeToKind(type);

		Key key;
		if (parent == null)
		{
			if (converted instanceof Long)
			{
				key = KeyFactory.createKey(kind, (Long) converted);
			}
			else
			{
				key = KeyFactory.createKey(kind, (String) converted);
			}
		}
		else
		{
			if (converted instanceof Long)
			{
				key = KeyFactory.createKey(parent, kind, (Long) converted);
			}
			else
			{
				key = KeyFactory.createKey(parent, kind, (String) converted);
			}
		}

		// needed to avoid sun generics bug "no unique maximal instance exists..."
		@SuppressWarnings("unchecked")
		T result = (T) load(key);
		return result;
	}

	public final <T> QueryResultIterator<T> find(Class<T> type)
	{
		return find().type(type).returnResultsNow();
	}

	public final <T> QueryResultIterator<T> find(Class<T> type, Object ancestor)
	{
		return find().type(type).withAncestor(ancestor).returnResultsNow();
	}

	public final <T> T toTypesafe(Entity entity)
	{
		@SuppressWarnings("unchecked") // needed for sun javac bug
		T result = (T) toTypesafe(entity, null);
		return result;
	}

	public <T> T toTypesafe(Entity entity, Predicate<String> predicate)
	{
		onBeforeDecode(entity);

		Type type = kindToType(entity.getKind());

		// filter out unwanted properties at the lowest level
		Map<String, Object> basic = entity.getProperties();
		if (predicate != null)
		{
			basic = Maps.filterKeys(basic, predicate);
		}

		Set<Property> properties = new PropertyMapToSet(basic, indexed);

		// order the properties for efficient separation by field
		properties = new TreeSet<Property>(properties);

		@SuppressWarnings("unchecked")
		T result = (T) translator.propertiesToTypesafe(properties, Path.EMPTY_PATH, type);
		if (result == null)
		{
			throw new IllegalStateException("Could not translate entity " + entity);
		}

		onAfterDecode(entity, result);

		return result;
	}

	public Query query(Type type)
	{
		return new Query(typeToKind(type));
	}

	protected RuntimeException newExceptionOnTranslateWrite(Exception e, Object instance)
	{
		return new RuntimeException(e);
	}

	public final <T> T load(Key key)
	{
		@SuppressWarnings("unchecked")
		T result = (T) keyToInstance(key, null);
		return result;
	}

	@SuppressWarnings("unchecked")
	protected <T> T keyToInstance(Key key, Predicate<String> propertyPredicate)
	{
		Entity entity = keyToEntity(key);
		if (entity == null)
		{
			return null;
		}
		else
		{
			return (T) toTypesafe(entity, propertyPredicate);
		}
	}

	protected Entity keyToEntity(Key key)
	{
		try
		{
			return serviceGet(key);
		}
		catch (EntityNotFoundException e)
		{
			return null;
		}
	}

	public final void update(Object instance, Key key)
	{
		// TODO share more code with store() - this is not needed now you can create
		// an Entity with a key
		Entity entity = new Entity(key);
		try
		{
			Set<Property> properties = translator.typesafeToProperties(instance, Path.EMPTY_PATH, indexed);
			if (properties == null)
			{
				throw new IllegalStateException("Could not translate instance: " + instance);
			}

			// copy in new properties
			transferProperties(entity, properties);
		}
		catch (Exception e)
		{
			throw newExceptionOnTranslateWrite(e, instance);
		}

		storeEntity(entity);

		onAfterUpdate(instance, entity);
	}

	protected void onAfterUpdate(Object instance, Entity entity)
	{
	}

	public final void deleteAll(Type type)
	{
		Query query = query(type);
		query.setKeysOnly();
		FetchOptions options = FetchOptions.Builder.withChunkSize(100);
		Iterator<Entity> entities = servicePrepare(query).asIterator(options);
		Iterator<Key> keys = Iterators.transform(entities, entityToKeyFunction);
		Iterator<List<Key>> partitioned = Iterators.partition(keys, 100);
		while (partitioned.hasNext())
		{
			deleteKeys(partitioned.next());
		}
	}

	protected final void deleteKeys(Collection<Key> keys)
	{
		serviceDelete(keys);
		onAfterDelete(keys);
	}

	// instance life-cycle extension points

	/**
	 * Called before every instance is translated from a types-safe object
	 * to a low-level datastore Entity. This must occur before storing
	 * or updating every instance.
	 * 
	 * @param instance
	 */
	protected void onBeforeEncode(Object instance)
	{
	}

	/**
	 * Called after every instance is translated from a types-safe object
	 * to a low-level datastore Entity. This must occur before storing
	 * or updating every instance.
	 * 
	 * @param instance
	 * @param entity
	 */
	protected void onAfterEncode(Object instance, Entity entity)
	{
	}
	

	/**
	 * Called after every entity returned from the datastore has been translated 
	 * into a type-safe instance.
	 * 
	 * @param entity
	 * @param instance
	 */
	protected void onAfterDecode(Entity entity, Object instance)
	{
	}

	/**
	 * Called before every entity returned from the datastore is translated into a
	 * type-safe instance.
	 *  
	 * @param entity
	 */
	protected void onBeforeDecode(Entity entity)
	{
	}

	/**
	 * Called after every instance that is deleted from the datastore.
	 * 
	 * @param keys
	 */
	protected void onAfterDelete(Collection<Key> keys)
	{
	}


	private static final Function<Entity, Key> entityToKeyFunction = new Function<Entity, Key>()
	{
		public Key apply(Entity arg0)
		{
			return arg0.getKey();
		}
	};
}
