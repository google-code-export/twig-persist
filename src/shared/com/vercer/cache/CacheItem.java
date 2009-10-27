package com.vercer.cache;

import java.io.Serializable;

import org.joda.time.DateTime;
import org.joda.time.Duration;

public class CacheItem<K, T> implements Serializable
{
	private static final DateTime NEVER = new DateTime(Long.MAX_VALUE);
	private static final Duration FOREVER = new Duration(Long.MAX_VALUE);
	private static final long serialVersionUID = 1L;
	private final T item;
	private final DateTime expirey;
	private final Duration duration;
	private final DateTime created;
	private DateTime accessed;
	private int accesses;
	private final K key;

	public CacheItem(K key, T value)
	{
		this(key, value, FOREVER);
	}

	public CacheItem(K key, T item, DateTime expirey)
	{
		this(key, item, expirey, FOREVER);
	}

	public CacheItem(K key, T item, Duration duration)
	{
		this(key, item, NEVER, duration);
	}

	private CacheItem(K key, T item, DateTime expirey, Duration duration)
	{
		this.key = key;
		this.item = item;
		this.expirey = expirey;
		this.duration = duration;

		created = new DateTime();
		accessed = created;
	}

	public T getValue()
	{
		this.accessed = new DateTime();
		this.accesses++;
		return this.item;
	}

	public DateTime getExpirey()
	{
		return this.expirey;
	}

	public Duration getDuration()
	{
		return this.duration;
	}

	public DateTime getCreated()
	{
		return this.created;
	}

	public DateTime getAccessed()
	{
		return this.accessed;
	}

	public int getAccesses()
	{
		return this.accesses;
	}

	public boolean isValid()
	{
		return expirey.isAfterNow() && accessed.plus(duration).isBeforeNow();
	}

	public K getKey()
	{
		return this.key;
	}
}
