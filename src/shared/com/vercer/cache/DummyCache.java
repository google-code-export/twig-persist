package com.vercer.cache;

import java.util.concurrent.Callable;

public class DummyCache<K, V> implements Cache<K, V>
{
	public V value(K key)
	{
		return null;
	}

	public V value(K key, Callable<CacheItem<K, V>> builder)
	{
		return null;
	}

	public CacheItem<K, V> item(K key)
	{
		return null;
	}

	public void invalidate(K key)
	{
	}

	public void invalidateAll()
	{
	}

	public void put(K key, V item)
	{
	}

	public void put(CacheItem<K, V> item)
	{
	}
}
