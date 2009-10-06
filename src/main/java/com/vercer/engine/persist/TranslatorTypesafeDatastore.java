package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.common.base.Function;
import com.google.appengine.repackaged.com.google.common.base.Nullable;
import com.google.appengine.repackaged.com.google.common.collect.Iterators;
import com.vercer.engine.persist.util.PropertyMapToSet;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.util.reference.ObjectReference;

/**
 *
 * @author John Patterson <john@vercer.com>
 */
public class TranslatorTypesafeDatastore implements TypesafeDatastore
{
	private final DatastoreService datastore;
	private PropertyTranslator translator;
	private boolean indexed;

	public TranslatorTypesafeDatastore(DatastoreService datastore, PropertyTranslator translator, boolean indexed)
	{
		this.datastore = datastore;
		this.translator = translator;
		this.indexed = indexed;
	}

	protected TranslatorTypesafeDatastore(DatastoreService datastore)
	{
		this.datastore = datastore;
	}

	protected void setPropertyTranslator(PropertyTranslator translator)
	{
		this.translator = translator;
	}

	protected void setIndexed(boolean indexed)
	{
		this.indexed = indexed;
	}

	public final Key store(Object instance, @Nullable Key parentKey, @Nullable String name)
	{
		onBeforeStore(instance);

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
			throw exceptionOnTranslateWrite(e, instance);
		}

		Key key = putEntityToDatstore(entity);

		onAfterStore(instance, entity);

		return key;
	}

	private void transferProperties(Entity entity, Collection<Property> properties)
	{
		for (Property property : properties)
		{
			// dereference object references
			Object value = property.getValue();
			if (value instanceof ObjectReference<?>)
			{
				value = ((ObjectReference<?>)value).get();
			}
			else if (value instanceof List<?>)
			{
				// we know the value is a mutable list from CollectionTranslator
				@SuppressWarnings("unchecked")
				List<Object> values = (List<Object>) value;
				for (int i = 0; i < values.size(); i++)
				{
					Object item = values.get(i);
					if (item instanceof ObjectReference<?>)
					{
						// dereference the value and set it in-place
						Object dereferenced = ((ObjectReference<?>) item).get();
						values.set(i, dereferenced);
					}
					else
					{
						// assume that they are all references or none
						break;
					}
				}
			}

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

	/**
	 * Give subclasses a chance to intercept all datastore puts
	 */
	protected Key putEntityToDatstore(Entity entity)
	{
		return datastore.put(entity);
	}

	public final Key store(Object instance, String name)
	{
		return store(instance, null, name);
	}

	public final Key store(Object value, Key parentKey)
	{
		return store(value, parentKey, null);
	}

	public final Key store(Object instance)
	{
		return store(instance, null, null);
	}

	public Object encode(Object object)
	{
		Set<Property> properties = translator.typesafeToProperties(object, Path.EMPTY_PATH, true);
		return properties.iterator().next().getValue();
	}

	protected Entity createEntity(KeySpecification spec)
	{
		if (spec.getParentKeyReference() == null)
		{
			if (spec.getName() == null)
			{
				return new Entity(spec.getKind());
			}
			else
			{
				return new Entity(spec.getKind(), spec.getName());
			}
		}
		else
		{
			Key key = spec.getParentKeyReference().get();
			if (spec.getName() == null)
			{
				return new Entity(spec.getKind(), key);
			}
			else
			{
				return new Entity(spec.getKind(), spec.getName(), key);
			}
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

	public final <T> T find(Class<T> type, Key parent, String name)
	{
		String kind = typeToKind(type);
		Key key = KeyFactory.createKey(parent, kind, name);

		// needed to avoid sun generics bug "no unique maximal instance exists..."
		@SuppressWarnings("unchecked")
		T result = (T) load(key);
		return result;
	}

	public final <T> T find(Class<T> type, String name)
	{
		return find(type, null, name);
	}

	public final <T> Iterable<T> find(final Class<T> type, final Key parent)
	{
		String kind = typeToKind(type);
		Query query = new Query(kind, parent);
		return find(query);
	}

	public final <T> Iterable<T> find(final Class<T> type)
	{
		String kind = typeToKind(type);
		Query query = new Query(kind);
		return find(query);
	}

	public <T> T toTypesafe(Entity entity)
	{
		onBeforeRestore(entity);

		Type type = kindToType(entity.getKind());

		Set<Property> properties = new PropertyMapToSet(entity.getProperties(), indexed);

		// order the properties for efficient separation by field
		properties = new TreeSet<Property>(properties);

		@SuppressWarnings("unchecked")
		T result = (T) translator.propertiesToTypesafe(properties, Path.EMPTY_PATH, type);
		if (result == null)
		{
			throw new IllegalStateException("Could not translate entity " + entity);
		}

		onAfterRestore(entity, result);

		return result;
	}

	protected void onAfterRestore(Entity entity, Object instance)
	{
	}

	protected void onBeforeRestore(Entity entity)
	{
	}


	public Query query(Class<?> clazz)
	{
		return new Query(typeToKind(clazz));
	}

	protected RuntimeException exceptionOnTranslateWrite(Exception e, Object instance)
	{
		return new RuntimeException(e);
	}

	protected void onBeforeStore(Object instance)
	{
	}

	protected void onAfterStore(Object instance, Entity entity)
	{
	}

	@SuppressWarnings("unchecked")
	public <T> T load(Key key)
	{
		Entity entity = keyToEntity(key);
		if (entity == null)
		{
			return null;
		}
		else
		{
			return (T) toTypesafe(entity);
		}
	}

	protected Entity keyToEntity(Key key)
	{
		try
		{
			return datastore.get(key);
		}
		catch (EntityNotFoundException e)
		{
			return null;
		}
	}

	public final DatastoreService getDatastore()
	{
		return datastore;
	}

	public final <T> Iterable<T> find(Query query)
	{
		if (query.isKeysOnly())
		{
			return new KeysOnlyQueryIterable<T>(query);
		}
		else
		{
			return new EntityQueryIterable<T>(query);
		}
	}

	public final void update(Object instance, Key key)
	{
		// TODO share more code with store()
		Entity entity = new Entity(key);
		try
		{
			Set<Property> properties = translator.typesafeToProperties(instance, Path.EMPTY_PATH, indexed);
			if (properties == null)
			{
				throw new IllegalStateException("Could not translate instance: " + instance);
			}

			// clear all existing properties
			Set<String> names = entity.getProperties().keySet();
			for (String name : names)
			{
				entity.removeProperty(name);
			}

			// copy in new properties
			transferProperties(entity, properties);
		}
		catch (Exception e)
		{
			throw exceptionOnTranslateWrite(e, instance);
		}

		putEntityToDatstore(entity);

		onAfterUpdate(instance, entity);
	}

	protected void onAfterUpdate(Object instance, Entity entity)
	{
	}

	public void delete(Key key)
	{
		datastore.delete(key);
		onAfterDelete(key);
	}

	protected void onAfterDelete(Key key)
	{
	}

	protected final class EntityQueryIterable<T> implements Iterable<T>
	{
		Iterable<Entity> iterable;

		private EntityQueryIterable(Query query)
		{
			assert query.isKeysOnly() == false;
			iterable = queryToEntityIterable(query);
		}

		public Iterator<T> iterator()
		{
			Iterator<Entity> iterator = iterable.iterator();
			return Iterators.transform(iterator, new EntityToInstanceFunction<T>());
		}
	}

	protected final class KeysOnlyQueryIterable<T> implements Iterable<T>
	{
		Iterable<Entity> iterable;

		private KeysOnlyQueryIterable(Query query)
		{
			assert query.isKeysOnly();
			iterable = queryToEntityIterable(query);
		}

		public Iterator<T> iterator()
		{
			Iterator<Entity> iterator = iterable.iterator();
			return Iterators.transform(iterator, new KeyToInstanceFunction<T>());
		}
	}

	protected Iterable<Entity> queryToEntityIterable(Query query)
	{
		PreparedQuery prepared = datastore.prepare(query);
		return prepared.asIterable();
	}

	protected final class EntityToInstanceFunction<T> implements Function<Entity, T>
	{
		@SuppressWarnings("unchecked")
		public T apply(Entity entity)
		{
			return (T) toTypesafe(entity);
		}
	}

	protected final class KeyToInstanceFunction<T> implements Function<Entity, T>
	{
		public T apply(Entity entity)
		{
			// needed to avoid sun generics bug "no unique maximal instance exists..."
			@SuppressWarnings("unchecked")
			T result = (T) load(entity.getKey());
			return result;
		}
	}
}
