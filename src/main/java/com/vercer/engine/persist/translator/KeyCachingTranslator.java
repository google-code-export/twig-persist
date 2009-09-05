package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.KeyCache;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.util.reference.ObjectReference;
import com.vercer.util.reference.SimpleObjectReference;

public final class KeyCachingTranslator extends DecoratingTranslator implements KeyCache
{
	private final Map<Key, Object> cacheByKey = new HashMap<Key, Object>();
	private final Map<Object, ObjectReference<Key>> cacheByValue = new IdentityHashMap<Object, ObjectReference<Key>>();

	public KeyCachingTranslator(PropertyTranslator chained)
	{
		super(chained);
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		Key key = (Key) properties.iterator().next().getValue();
		if (cacheByKey.containsKey(key))
		{
			return cacheByKey.get(key);
		}
		else
		{
			Object object = chained.propertiesToTypesafe(properties, path, type);
			cache(key, object);

			return object;
		}
	}

	public void cache(Key key, Object object)
	{
		cacheByKey.put(key, object);
		SimpleObjectReference<Key> reference = new SimpleObjectReference<Key>(key);
		cacheByValue.put(object, reference);
	}

	public void cacheKeyReferenceForInstance(Object object, ObjectReference<Key> keyReference)
	{
		if (cacheByValue.put(object, keyReference) != null)
		{
			throw new IllegalStateException("Object already existed: " + object);
		}
	}

	public Set<Property> typesafeToProperties(Object object, Path path, boolean indexed)
	{
		// return from cache if already exists
		if (cacheByValue.containsKey(object))
		{
			Key key = cacheByValue.get(object).get();
			Property property = new SimpleProperty(path, key, indexed);
			return Collections.singleton(property);
		}
		else
		{
			// get the key and cache it
			Set<Property> properties = chained.typesafeToProperties(object, path, indexed);
			Key key = (Key) properties.iterator().next().getValue();
			cache(key, object);
			return properties;
		}
	}

	public void clearKeyCache()
	{
		this.cacheByKey.clear();
		this.cacheByValue.clear();
	}

	public Key evictEntity(Object reference)
	{
		ObjectReference<Key> keyReference = cacheByValue.remove(reference);
		Key key = keyReference.get();
		Object removed = cacheByKey.remove(key);
		assert removed == reference;
		return key;
	}

	public Object evictKey(Key key)
	{
		Object object = cacheByKey.remove(key);
		ObjectReference<Key> removed = cacheByValue.remove(object);
		assert removed.get() == key;
		return object;
	}


}
