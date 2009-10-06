package com.vercer.engine.persist.util;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.KeyCache;
import com.vercer.util.reference.ObjectReference;
import com.vercer.util.reference.SimpleObjectReference;

public class SimpleKeyCache implements KeyCache
{
	final Map<Key, Object> cacheByKey = new HashMap<Key, Object>();
	final Map<Object, ObjectReference<Key>> cacheByValue = new IdentityHashMap<Object, ObjectReference<Key>>();

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

	@SuppressWarnings("unchecked")
	public <T> T getCachedEntity(Key key)
	{
		return (T) cacheByKey.get(key);
	}

	public Key getCachedKey(Object entity)
	{
		ObjectReference<Key> reference = cacheByValue.get(entity);
		if (reference != null)
		{
			return reference.get();
		}
		else
		{
			return null;
		}
	}

}
