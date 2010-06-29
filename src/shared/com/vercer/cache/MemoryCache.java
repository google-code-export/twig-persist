package com.vercer.cache;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MemoryCache<K, V> implements Cache<K, V>
{
	private static final Logger logger = Logger.getLogger(MemoryCache.class.getName());

	private ReadWriteLock lock = new ReentrantReadWriteLock();
	private final LinkedHashMap<K, CacheItem<K, V>> items;
	private final ConcurrentMap<K, K> keys;

	private final int capacity;
	private final boolean prune;
	private final boolean intern;

	/**
	 * @param capacity Maximum items to store
	 * @param prune Should we check for expired items after <code>capacity</code> operations
	 * @param intern Set to true if key items are not unique instances
	 */
	public MemoryCache(int capacity)
	{
		this(capacity, false, true);
	}

	public MemoryCache(int capacity, boolean prune, boolean intern)
	{
		this.capacity = capacity;
		this.prune = prune;
		this.intern = intern;

		if (intern)
		{
			keys = new ConcurrentHashMap<K, K>();
		}
		else
		{
			keys = null;
		}

		this.items = new LinkedHashMap<K, CacheItem<K, V>>(capacity, 0.75f, true)
		{
			private static final long serialVersionUID = 1L;
			private final int check = MemoryCache.this.capacity;
			private int operations;

			@Override
			protected boolean removeEldestEntry(Map.Entry<K, CacheItem<K, V>> eldest)
			{
				// free some memory by looking for expired items
				if (MemoryCache.this.prune && operations++ >= check)
				{
					logger.info("Checking cache for expired items. Operation " + operations);
					Iterator<CacheItem<K, V>> iterator = this.values().iterator();
					while (iterator.hasNext())
					{
						CacheItem<K, V> item = iterator.next();
						if (item.isValid() == false)
						{
							if (logger.isLoggable(Level.FINE))
							{
								logger.fine("Removing invalid cache item from memory: " + item);
							}
							iterator.remove();
						}
					}
					operations = 0;
				}

				boolean remove = size() > MemoryCache.this.capacity;

				logger.fine("Remove eldest item: " + remove);

				return remove;
			}
		};

	}


	public void put(K key, V value)
	{
		put(new CacheItem<K, V>(key, value));
	}


	public void put(CacheItem<K, V> item)
	{
		if (intern)
		{
			// do not replace an existing key instance
			keys.putIfAbsent(item.getKey(), item.getKey());
		}

		lock.writeLock().lock();
		items.put(item.getKey(), item);
		lock.writeLock().unlock();
	}


	public V value(K key)
	{
		CacheItem<K, V> item = item(key);
		if (item != null)
		{
			if (item.isValid())
			{
				return item.getValue();
			}
			else
			{
				invalidate(key);
			}
		}

		return null;
	}


	public CacheItem<K, V> item(K key)
	{
		lock.readLock().lock();
		CacheItem<K, V> result = items.get(key);
		lock.readLock().unlock();

		return result;
	}

	public V value(K key, Callable<CacheItem<K, V>> builder)
	{
		V v = value(key);
		if (v != null)
		{
			if (logger.isLoggable(Level.FINE))
			{
				logger.fine("Got item from cache: " + v);
			}
			return v;
		}
		else
		{
			// we will lock on the key so there can only be one thread updating the same key
			K subject = null;
			if (intern)
			{
				// make sure we lock on the same instance of the key
				subject = keys.putIfAbsent(key, key);
			}

			if (subject == null)
			{
				subject = key;
			}

			if (logger.isLoggable(Level.FINE))
			{
				logger.fine("About to create item for key: " + key);
			}

			// lock updates for this key so other thread don't create the same item
			synchronized (subject)
			{
				// another thread may have already created the item while we were waiting
				v = value(key);
				if (v != null)
				{
					// another thread created or refreshed the item

					if (logger.isLoggable(Level.FINE))
					{
						logger.fine("Another thread created item for key: " + key);
					}
					return v;
				}
				else
				{
					// build the item - could take some time
					CacheItem<K, V> item;
					try
					{
						item = builder.call();

						if (logger.isLoggable(Level.FINE))
						{
							logger.fine("Built item for key: " + key);
						}
					}
					catch (Exception e)
					{
						if (e instanceof RuntimeException)
						{
							throw (RuntimeException) e;
						}
						throw new RuntimeException(e);
					}

					if (item == null)
					{
						throw new IllegalStateException("Cache item cannot be null");
					}
					else
					{
						lock.writeLock().lock();
						items.put(key, item);
						lock.writeLock().unlock();

						if (logger.isLoggable(Level.FINE))
						{
							logger.fine("Put item in cache for key: " + key);
						}
					}
					return item.getValue();
				}
			}
		}

	};

	public int getCapacity()
	{
		return this.capacity;
	}


	public void invalidate(K key)
	{
		lock.writeLock().lock();
		items.remove(key);
		if (intern)
		{
			keys.remove(key);
		}
		lock.writeLock().unlock();
	}


	public void invalidateAll()
	{
		lock.writeLock().lock();
		items.clear();
		if (intern)
		{
			keys.clear();
		}
		lock.writeLock().unlock();
	}
}
