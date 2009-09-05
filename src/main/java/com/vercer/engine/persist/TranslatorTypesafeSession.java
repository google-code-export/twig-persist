package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.common.base.Nullable;
import com.vercer.engine.persist.util.MapToPropertySet;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.util.reference.ObjectReference;

/**
 * A thin wrapper around app engines datastore providing type safe data access.
 * Must be configured at creation time by overriding methods so that they are
 * immutable.
 *
 * @author John Patterson <john@vercer.com>
 */
public class TranslatorTypesafeSession implements TypesafeSession
{
	private final DatastoreService datastore;
	private PropertyTranslator translator;
	private boolean indexed;

	public TranslatorTypesafeSession(DatastoreService datastore, PropertyTranslator translator, boolean indexed)
	{
		this.datastore = datastore;
		this.translator = translator;
		this.indexed = indexed;
	}

	protected TranslatorTypesafeSession(DatastoreService datastore)
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
		onBeforeSave(instance);

		Entity entity;
		try
		{
			Collection<Property> properties = translator.typesafeToProperties(instance, Path.EMPTY_PATH, indexed);
			if (properties == null)
			{
				throw new IllegalStateException("Could not translate instance: " + instance);
			}

			String kind = typeToKind(instance.getClass());

			entity = createEntity(new KeySpecification(kind, parentKey, name));

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
		catch (RuntimeException e)
		{
			throw exceptionOnTranslateWrite(e, instance);
		}

		Key key = put(entity);

		onAfterStore(instance, key);

		return key;
	}

	protected Key put(Entity entity)
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

	public final <T> T find(String name, Class<T> type, Key parent)
	{
		Key key = KeyFactory.createKey(parent, type.getName(), name);
		Entity entity;
		try
		{
			entity = datastore.get(key);
		}
		catch (EntityNotFoundException e)
		{
			return null;
		}

		@SuppressWarnings("unchecked")
		T result = (T) toTypesafe(entity);
		return result;
	}


	public final Object toTypesafe(Entity entity)
	{
		onBeforeRestore(entity);
		Type type = kindToType(entity.getKind());
		Set<Property> properties = new MapToPropertySet(entity.getProperties(), indexed);
		Object result = translator.propertiesToTypesafe(properties, Path.EMPTY_PATH, type);
		if (result == null)
		{
			throw new IllegalStateException("Could not translate entity " + entity);
		}
		onAfterRestore(entity, result);
		return result;
	}

	protected RuntimeException exceptionOnTranslateWrite(RuntimeException e, Object instance)
	{
		return e;
	}

	protected void onBeforeSave(Object instance)
	{
	}

	protected void onAfterStore(Object instance, Key key)
	{
	}

	protected void onAfterRestore(Entity entity, Object instance)
	{
	}

	protected void onBeforeRestore(Entity entity)
	{
	}

	public final <T> T find(String name, Class<T> type)
	{
		return find(name, type, null);
	}

	public final Object load(Key key)
	{
		try
		{
			return toTypesafe(datastore.get(key));
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

	public final <T> List<T> find(Query query)
	{
		return null;
	}

	public final void delete(Key key)
	{
		// TODO Auto-generated method stub

	}

	public final void update(Object value)
	{
		// TODO Auto-generated method stub

	}

	public final void update(Object value, Key key)
	{
		// TODO Auto-generated method stub

	}

	public final void delete(Object instance)
	{
		// TODO Auto-generated method stub

	}
}
