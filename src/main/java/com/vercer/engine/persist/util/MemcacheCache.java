package com.vercer.engine.persist.util;

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
	private static final int CHECK_LOCK_INTERVAL_MILIS = 200;

	private final MemcacheService memcache;

	@Inject
	public MemcacheCache(MemcacheService memcache)
	{
		this.memcache = memcache;
	}

	public void invalidate(K key)
	{
		memcache.delete(key);
	}

	public void invalidateAll()
	{
		memcache.clearAll();
	}

	@SuppressWarnings("unchecked")
	public CacheItem<K, V> item(K key)
	{
		return (CacheItem<K, V>) memcache.get(key);
	}

	public void put(K key, V item)
	{
		put(new CacheItem<K, V>(key, item));
	}

	public void put(CacheItem<K, V> item)
	{
		memcache.put(item.getKey(), item);
	}

	@SuppressWarnings("unchecked")
	public V value(K key)
	{
		long start = System.currentTimeMillis();
		while (System.currentTimeMillis() - start < LOCK_TIMEOUT_MILLIS)
		{
			Object object = memcache.get(key);
			if (!LOCK.equals(object))
			{
				return ((CacheItem<K,V>) object).getValue();
			}
			try
			{
				Thread.sleep(CHECK_LOCK_INTERVAL_MILIS);
			}
			catch (InterruptedException e)
			{
				throw new IllegalStateException(e);
			}
		}
		throw new IllegalStateException("Timeout waiting for cache lock");
	}

	public V value(K key, Callable<CacheItem<K, V>> builder)
	{
		if (memcache.put(key, LOCK, Expiration.byDeltaMillis(LOCK_TIMEOUT_MILLIS), SetPolicy.ADD_ONLY_IF_NOT_PRESENT))
		{
			// there was nothing there
			try
			{
				CacheItem<K,V> item = builder.call();
				if (item == null)
				{
					throw new IllegalStateException("Item cannot be null from " + builder);
				}
				memcache.put(key, item);
				return item.getValue();
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}
		else
		{
			// there was something there already
			return value(key);
		}
	}

}
