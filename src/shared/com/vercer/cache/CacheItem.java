package com.vercer.cache;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class CacheItem<K, T> implements Serializable
{
	private static final Date NEVER = new Date(Long.MAX_VALUE);
	private static final long serialVersionUID = 1L;
	private final T item;
	private final Date expirey;
	private final long duration;
	private final TimeUnit unit;
	private final Date created;
	private Date accessed;
	private int accesses;
	private final K key;

	public CacheItem(K key, T value)
	{
		this(key, value, 0, null);
	}

	public CacheItem(K key, T item, Date expirey)
	{
		this(key, item, expirey, 0, null);
	}

	public CacheItem(K key, T item, long duration, TimeUnit unit)
	{
		this(key, item, NEVER, duration, unit);
	}

	private CacheItem(K key, T item, Date expirey, long duration, TimeUnit unit)
	{
		this.key = key;
		this.item = item;
		this.expirey = expirey;
		this.duration = duration;
		this.unit = unit;

		created = new Date();
		accessed = created;
	}

	public T getValue()
	{
		this.accessed = new Date();
		this.accesses++;
		return this.item;
	}

	public Date getExpirey()
	{
		return this.expirey;
	}

	public long getDuration()
	{
		return this.duration;
	}
	
	public TimeUnit getUnit()
	{
		return unit;
	}

	public Date getCreated()
	{
		return this.created;
	}

	public Date getAccessed()
	{
		return this.accessed;
	}

	public int getAccesses()
	{
		return this.accesses;
	}

	public boolean isValid()
	{
		Date now = new Date();
		return expirey.after(now) && (unit == null || duration == Long.MAX_VALUE || new Date(accessed.getTime() + unit.toMillis(duration)).after(now));
	}

	public K getKey()
	{
		return this.key;
	}
}
