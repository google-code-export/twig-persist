package com.vercer.cache;

import java.util.concurrent.Callable;

public interface Cache<K, V>
{
	void put(K key, V item);
	void put(CacheItem<K, V> item);
	V value(K key);
	V value(K key, Callable<CacheItem<K, V>> builder);
	CacheItem<K, V> item(K key);
	void invalidate(K key);
}
