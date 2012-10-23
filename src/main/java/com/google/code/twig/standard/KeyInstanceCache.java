package com.google.code.twig.standard;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.util.reference.ObjectReference;
import com.google.code.twig.util.reference.SimpleObjectReference;
import com.google.common.collect.MapMaker;

// TODO make this the base class of translator object datastore
public class KeyInstanceCache
{
	public static class KeyReference extends SimpleObjectReference<Key>
	{
		private static final long serialVersionUID = 1L;
		private long version;
		
		public KeyReference(Key object)
		{
			super(object);
		}
	}
	
	// weak values remove mapping when instance is no longer in use
	// Key is looked up by using equals() 
	// concurrency is not needed so set to 1
	// TODO replace this with CacheMaker
	private Map<Key, Object> keyToInstance = new MapMaker()
		.weakValues()
		.concurrencyLevel(1)
		.makeMap();

	// weak keys remove the value (Key) when instance not needed
	// better than WeakHashMap because uses identity key comparison
	// use a reference so we can hold a Key or a KeySpecification
	private Map<Object, KeyReference> instanceToKeyReference = new MapMaker()
		.weakKeys()
		.concurrencyLevel(1)
		.makeMap();

	/**
	 * Used for both encoding and decoding. During decoding this is
	 * called immediately before the instance is activated. During encoding
	 * it is called after {@link #cacheKeyReferenceForInstance(Object, ObjectReference)}
	 * only once the instance is put and the real Key is known.
	 * @param activated Is this instance activated
	 */
	public void cache(Key key, Object object, long version)
	{
		keyToInstance.put(key, object);
		KeyReference reference = new KeyReference(key);
		reference.version = version;
		instanceToKeyReference.put(object, reference);
	}
	
	/**
	 * Used during encoding before we know the @Id and @Parent
	 */
	public void cacheKeyReferenceForInstance(Object object, KeyReference keyReference)
	{
		if (instanceToKeyReference.put(object, keyReference) != null)
		{
			throw new IllegalStateException("Object already existed: " + object);
		}
	}

	
	/**
	 * Clears all Keys and instances which effectively resets the ObjectDatastore 
	 */
	public void clear()
	{
		this.keyToInstance.clear();
		this.instanceToKeyReference.clear();
	}
	
	/**
	 * Keys are changed when they are stored which alters their hash value
	 * making it impossible to look up the instance by key.  This re-hashs
	 * each entry so values can be looked up again.
	 */
	public void rehashKeys()
	{
		ConcurrentMap<Key, Object> replacement = new MapMaker()
			.weakValues()
			.concurrencyLevel(1)
			.makeMap();
		
		replacement.putAll(keyToInstance);
		
		keyToInstance = replacement;
	}

	/**
	 * Both the Key and the instance will be removed from the cache automatically
	 * when the instance is no longer referenced and the garbage collector runs.
	 * You can help free up memory by explicitly evicting the instance and Key
	 * before this happens.
	 */
	public Key evictInstance(Object reference)
	{
		KeyReference keyReference = instanceToKeyReference.remove(reference);
		if (keyReference != null)
		{
			Key key = keyReference.get();
			keyToInstance.remove(key);
			return key;
		}
		else
		{
			return null;
		}
	}

	/**
	 * @see {@link #evictInstance(Object)}
	 */
	public Object evictKey(Key key)
	{
		Object object = keyToInstance.remove(key);
		if (object == null)
		{
			throw new NoSuchElementException("Key " + key + " was not cached");
		}
		instanceToKeyReference.remove(object);
		return object;
	}

	/**
	 * @return The instance associated with this Key
	 */
	@SuppressWarnings("unchecked")
	public <T> T getInstance(Key key)
	{
		return (T) keyToInstance.get(key);
	}

	/**
	 * @return The Key associated with this instance.
	 * @throws RuntimeException if the Key specification is not complete
	 */
	public Key getKey(Object instance)
	{
		KeyReference reference = instanceToKeyReference.get(instance);
		if (reference != null)
		{
			return reference.get();
		}
		else
		{
			return null;
		}
	}
	
	/**
	 * @return The Key associated with this instance.
	 * @throws RuntimeException if the Key specification is not complete
	 */
	public long version(Object instance)
	{
		KeyReference reference = instanceToKeyReference.get(instance);
		if (reference != null)
		{
			return reference.version;
		}
		else
		{
			throw new IllegalArgumentException("Instance is not associated: " + instance);
		}
	}
	
	public void setVersion(Object instance, long version)
	{
		KeyReference reference = instanceToKeyReference.get(instance);
		if (reference != null)
		{
			if (reference.version != 0 && version != Math.abs(reference.version) + 1)
			{
				throw new IllegalStateException("Version must increment");
			}
			reference.version = version;
		}
		else
		{
			throw new IllegalArgumentException("Instance is not associated: " + instance);
		}
	}
	
	/**
	 * Useful when you need to know if the instance is already associated
	 * but when the Key might not yet be complete (during encoding)
	 * @return The reference to the Key which may not be complete.
	 */
	public KeyReference getKeyReference(Object instance)
	{
		return instanceToKeyReference.get(instance);
	}

	public Set<Key> getAllKeys()
	{
		return keyToInstance.keySet();
	}
	
	public boolean isActivated(Object instance)
	{
		KeyReference reference = (KeyReference) instanceToKeyReference.get(instance);
		if (reference == null)
		{
			throw new IllegalArgumentException("Object is not an associated instance: " + instance);
		}
		return reference.version != 0;
	}
	
	public boolean isActivatable(Object instance)
	{
		KeyReference reference = (KeyReference) instanceToKeyReference.get(instance);
		if (reference == null)
		{
			return false;
		}
		return reference.version == 0;
	}

	public boolean containsKey(Key key)
	{
		return keyToInstance.containsKey(key);
	}
}
