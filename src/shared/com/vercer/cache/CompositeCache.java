package com.vercer.cache;

import java.util.concurrent.Callable;


public class CompositeCache<K, V> implements Cache<K, V>
{
	private Cache<K, V>[] delegates;

	public CompositeCache(Cache<K, V>... delegates)
	{
		this.delegates = delegates;
	}

	public void invalidate(K key)
	{
		for (Cache<K, V> delegate : delegates)
		{
			delegate.invalidate(key);
		}
	}

	public CacheItem<K, V> item(K key)
	{
		for (Cache<K, V> delegate : delegates)
		{
			CacheItem<K,V> item = delegate.item(key);
			if (item != null)
			{
				// add the item to any higher delegates that missed
				for (Cache<K, V> previous : delegates)
				{
					if (previous == delegate) break;
					previous.put(item);
				}
				return item;
			}
		}
		return null;
	}

	public void put(K key, V item)
	{
		for (Cache<K, V> delegate : delegates)
		{
			delegate.put(key, item);
		}
	}

	public void put(CacheItem<K, V> item)
	{
		for (Cache<K, V> delegate : delegates)
		{
			delegate.put(item);
		}
	}

	public V value(K key)
	{
		CacheItem<K, V> item = item(key);
		if(item != null)
		{
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
		V value = value(key);
		if (value == null)
		{
			// use the first to lock while building value
			value = delegates[0].value(key, builder);
			
			// just put the built value in the rest
			for (int i = 1; i < delegates.length; i++)
			{
				delegates[i].put(key, value);
			}
		}
		return value;
	}

}
