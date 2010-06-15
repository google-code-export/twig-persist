package com.vercer.engine.persist.standard;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.appengine.api.datastore.Key;
import com.google.common.collect.MapMaker;
import com.vercer.util.reference.ObjectReference;
import com.vercer.util.reference.SimpleObjectReference;

public class KeyCache
{
	private static class ActivatableKeyReference extends SimpleObjectReference<Key>
	{
		private static final long serialVersionUID = 1L;
		private boolean activated;
		public ActivatableKeyReference(Key object)
		{
			super(object);
		}
	}
	
	private Map<Key, Object> cacheByKey = new MapMaker()
		.weakValues()
		.concurrencyLevel(1)
		.makeMap();


	private Map<Object, ObjectReference<Key>> cacheByValue = new MapMaker()
			.weakKeys()
			.concurrencyLevel(1)
			.makeMap();

	public void cache(Key key, Object object)
	{
		cacheByKey.put(key, object);
		SimpleObjectReference<Key> reference = new ActivatableKeyReference(key);
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

	public Key evictInstance(Object reference)
	{
		ObjectReference<Key> keyReference = cacheByValue.remove(reference);
		if (keyReference != null)
		{
			Key key = keyReference.get();
			cacheByKey.remove(key);
			return key;
		}
		else
		{
			return null;
		}
	}

	public Object evictKey(Key key)
	{
		Object object = cacheByKey.remove(key);
		if (object == null)
		{
			throw new NoSuchElementException("Key " + key + " was not cached");
		}
		ObjectReference<Key> removed = cacheByValue.remove(object);
		assert removed.get() == key;
		return object;
	}

	@SuppressWarnings("unchecked")
	public <T> T getCachedInstance(Key key)
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
	
	public Set<Key> getAllKeys()
	{
		return cacheByKey.keySet();
	}
	
	public Key getCachedKeyAndActivate(Object entity)
	{
		// we are sure of the key reference type because the full key and instance must have been added 
		ActivatableKeyReference reference = (ActivatableKeyReference) cacheByValue.get(entity);
		if (reference != null)
		{
			if (reference.activated)
			{
				throw new IllegalStateException("Instance was already activated");
			}
			reference.activated = true;
			return reference.get();
		}
		else
		{
			return null;
		}
	}

	public boolean containsKey(Key key)
	{
		return cacheByKey.containsKey(key);
	}

}
