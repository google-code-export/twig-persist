package com.vercer.engine.util;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheService.SetPolicy;
import com.google.inject.Inject;
import com.vercer.cache.Cache;
import com.vercer.cache.CacheItem;

public class MemcacheCache<K, V> implements Cache<K, V>
{
	private static final Object LOCK = "initialising";
	private static final int LOCK_TIMEOUT_MILLIS = 60000;
	private final int retries;
	private final long retryMillis;
	private final String name;

	private final MemcacheService memcache;

	/**
	 * Defaults to retrying blocked items for 10 seconds every 0.5 seconds
	 * @param memcache
	 */
	@Inject
	public MemcacheCache(MemcacheService memcache)
	{
		this(memcache, "default");
	}
	
	public MemcacheCache(MemcacheService memcache, String name)
	{
		this(memcache, name, 500, 20);
	}

	public MemcacheCache(MemcacheService memcache, String name, long retryMillis, int retries)
	{
		this.memcache = memcache;
		this.retryMillis = retryMillis;
		this.retries = retries;
		this.name = name;
	}

	public void invalidate(K key)
	{
		memcache.delete(new NamedKey(name, key));
	}

	@SuppressWarnings("unchecked")
	public CacheItem<K, V> item(K key)
	{
		Object item = memcache.get(new NamedKey(name, key));
		if (item == null || item.equals(LOCK))
		{
			return null;
		}
		else
		{
			return (CacheItem<K, V>) item;
		}
	}

	public void put(K key, V item)
	{
		put(newDefaultCacheItem(key, item));
	}

	protected CacheItem<K, V> newDefaultCacheItem(K key, V item)
	{
		return new CacheItem<K, V>(key, item);
	}

	public void put(CacheItem<K, V> item)
	{
		memcache.put(new NamedKey(name, item.getKey()), item);
	}

	@SuppressWarnings("unchecked")
	public V value(K key)
	{
		Object object = memcache.get(new NamedKey(name, key));
		if (object == null)
		{
			return null;
		}
		else if (!LOCK.equals(object))
		{
			CacheItem<K,V> item = (CacheItem<K,V>) object;
			if (item.isValid())
			{
				return item.getValue();
			}
			else
			{
				invalidate(key);
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	private static class NamedKey implements Serializable
	{
		private static final long serialVersionUID = 1L;
		
		String name;
		Object value;
		
		public NamedKey(String name, Object value)
		{
			this.name = name;
			this.value = value;
		}
		
		@Override
		public String toString()
		{
			return "NamedKey [name=" + name + ", value=" + value + "]";
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((value == null) ? 0 : value.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			NamedKey other = (NamedKey) obj;
			if (name == null)
			{
				if (other.name != null)
					return false;
			}
			else if (!name.equals(other.name))
				return false;
			if (value == null)
			{
				if (other.value != null)
					return false;
			}
			else if (!value.equals(other.value))
				return false;
			return true;
		}

		
	}
	
	public V value(K key, Callable<CacheItem<K, V>> builder)
	{
		if (memcache.put(new NamedKey(name, key), LOCK, Expiration.byDeltaMillis(LOCK_TIMEOUT_MILLIS), SetPolicy.ADD_ONLY_IF_NOT_PRESENT))
		{
			// there was nothing there so we put the lock
			try
			{
				CacheItem<K,V> item = builder.call();
				if (item == null)
				{
					throw new IllegalStateException("Item cannot be null from " + builder);
				}
				memcache.put(new NamedKey(name, key), item);
				return item.getValue();
			}
			catch (Exception e)
			{
				// TODO remove a separate lock key
				memcache.delete(new NamedKey(name, key)); // remove the lock
				throw new IllegalStateException("Could not put value for key " + key, e);
			}
		}
		else
		{
			// there was already something in the cache for the key
			Object object;
			for (int retry = 0; retry < retries; retry++)
			{
				object = memcache.get(new NamedKey(name, key));
				if (object == null)
				{
					// how can this be?
					return null;
				}
				else if (object.equals(LOCK))
				{
					// cache key is locked so wait
					try
					{
						Thread.sleep(retryMillis);
					}
					catch (InterruptedException e)
					{
						throw new IllegalStateException(e);
					}
				}
				else
				{
					@SuppressWarnings("unchecked")
					CacheItem<K,V> item = (CacheItem<K,V>) object;
					return item.getValue();
				}
			}
			throw new IllegalStateException("Cache timeout");
		}
	}

}
