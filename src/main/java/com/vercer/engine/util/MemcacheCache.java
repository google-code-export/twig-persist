package com.vercer.engine.util;

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

	private final MemcacheService memcache;

	@Inject
	public MemcacheCache(MemcacheService memcache)
	{
		this(memcache, 500, 20);
	}

	public MemcacheCache(MemcacheService memcache, long retryMillis, int retries)
	{
		this.memcache = memcache;
		this.retryMillis = retryMillis;
		this.retries = retries;
	}

	public void invalidate(K key)
	{
		memcache.delete(storeKeyValue(key));
	}

	protected Object storeKeyValue(K key)
	{
		return key;
	}

	@SuppressWarnings("unchecked")
	public CacheItem<K, V> item(K key)
	{
		Object item = memcache.get(storeKeyValue(key));
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
		memcache.put(storeKeyValue(item.getKey()), item);
	}

	@SuppressWarnings("unchecked")
	public V value(K key)
	{
		Object object = memcache.get(storeKeyValue(key));
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

	public V value(K key, Callable<CacheItem<K, V>> builder)
	{
		if (memcache.put(storeKeyValue(key), LOCK, Expiration.byDeltaMillis(LOCK_TIMEOUT_MILLIS), SetPolicy.ADD_ONLY_IF_NOT_PRESENT))
		{
			// there was nothing there so we put the lock
			try
			{
				CacheItem<K,V> item = builder.call();
				if (item == null)
				{
					throw new IllegalStateException("Item cannot be null from " + builder);
				}
				memcache.put(storeKeyValue(key), item);
				return item.getValue();
			}
			catch (Exception e)
			{
				// TODO remove a separate lock key
				memcache.delete(storeKeyValue(key)); // remove the lock
				throw new IllegalStateException("Could not put value for key " + key, e);
			}
		}
		else
		{
			// there was already something in the cache for the key
			Object object;
			for (int retry = 0; retry < retries; retry++)
			{
				object = memcache.get(storeKeyValue(key));
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
