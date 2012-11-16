package com.google.code.twig.standard;

import static com.google.common.base.Predicates.in;
import static com.google.common.base.Predicates.not;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.ReadPolicy;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.memcache.AsyncMemcacheService;
import com.google.appengine.api.memcache.Expiration;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.code.twig.LoadCommand.CacheMode;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.Settings;
import com.google.code.twig.Work;
import com.google.common.base.Predicates;
import com.google.common.collect.Collections2;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Maps;
import com.google.common.util.concurrent.AbstractFuture;


/**
 * TODO make all operations use async datastore service
 *
 * TODO this should not be a super class but a plugable component
 *
 * TODO extend future to allow a value() with no checked throwing
 *
 * @author John Patterson (john@vercer.com)
 */
public abstract class BaseObjectDatastore implements ObjectDatastore
{
	private DatastoreService defaultDatastoreService;
	private Transaction transaction;
	private static final Logger logger = Logger.getLogger(BaseObjectDatastore.class.getName());

	private Map<Key, Entity> batched;
	private int threshold = Integer.MAX_VALUE;

	private static final String MEMCACHE_PREFIX = "__twig";
	private static AsyncMemcacheService memcache;

	public static class Statistics
	{
		int puts;
		int gets;
		int queries;

		int memcacheGets;
		int memcachePuts;
		int datastoreGets;

		public int getGets()
		{
			return this.gets;
		}
		public int getPuts()
		{
			return this.puts;
		}
		public int getDatastoreGets()
		{
			return this.datastoreGets;
		}

		@Override
		public String toString()
		{
			return "Statistics [puts=" + this.puts + ", gets=" + this.gets + ", queries="
					+ this.queries + ", memcacheGets=" + this.memcacheGets + ", memcachePuts="
					+ this.memcachePuts + ", datastoreGets=" + this.datastoreGets + "]";
		}
	}

	// TODO move statics into registry
	// concurrency not a problem if configured once in during startup
	private static final Map<String, CacheDetails> kindToCache = Maps.newHashMap();

	final Statistics statistics = new Statistics();
	private Settings defaultSettings;

	public Statistics getStatistics()
	{
		return this.statistics;
	}

	public static void registerCachedKind(String kind, int seconds, int maximum, boolean automatic, boolean global)
	{
		Map<Key, Entity> cache = null;
		if (maximum >= 0)
		{
			// 10 concurrent threads will not be all updating at the same time
			MapMaker maker = new MapMaker()
				.concurrencyLevel(5)
				.softValues();

			if (seconds > 0)
			{
				maker.expiration(seconds, TimeUnit.SECONDS);
			}
			if (maximum > 0)
			{
				maker.maximumSize(maximum);
			}

			cache = maker.makeMap();
		}

		kindToCache.put(kind, new CacheDetails(cache, seconds, maximum, automatic, global));
	}

	public static boolean isKindCached(String kind)
	{
		return kindToCache.containsKey(kind);
	}

	// TODO create public cache options with builder
	public static class CacheDetails
	{
		private final boolean global;
		private final Map<Key, Entity> cache;
		private final int seconds;
		private final int maximum;
		private final boolean automatic;

		// TODO write a filter to log these statistics
//		private final AtomicInteger memcacheHits = new AtomicInteger();
//		private final AtomicInteger memoryHits = new AtomicInteger();
//		private final AtomicInteger reads = new AtomicInteger();
//		private final AtomicInteger memcacheWrites = new AtomicInteger();

		public CacheDetails(Map<Key, Entity> cache, int seconds, int maximum, boolean automatic, boolean global)
		{
			this.cache = cache;
			this.seconds = seconds;
			this.maximum = maximum;
			this.automatic = automatic;
			this.global = global;
		}

//		public int getMemcacheHits()
//		{
//			return this.memcacheHits.get();
//		}
//
//		public int getMemoryHits()
//		{
//			return this.memoryHits.get();
//		}
//
//		public int getReads()
//		{
//			return this.reads.get();
//		}
//
//		public int getMemcacheWrites()
//		{
//			return this.memcacheWrites.get();
//		}

		public int getSeconds()
		{
			return this.seconds;
		}

		public int getMaximum()
		{
			return this.maximum;
		}
	}

	public BaseObjectDatastore(Settings settings)
	{
		this.defaultSettings = settings;
		this.defaultDatastoreService = newDatastoreService(settings);
	}

	private DatastoreService newDatastoreService(Settings settings)
	{
		DatastoreServiceConfig config = DatastoreServiceConfig.Builder.withDefaults();
		if (settings.getDeadline() != null)
		{
			config.deadline(settings.getDeadline());
		}
		if (settings.getConsistency() != null)
		{
			config.readPolicy(new ReadPolicy(settings.getConsistency()));
		}

		return DatastoreServiceFactory.getDatastoreService(config);
	}

	public Settings getDefaultSettings()
	{
		return this.defaultSettings;
	}

	@Override
	public void startBatchMode()
	{
		if (batched == null)
		{
			batched = new HashMap<Key, Entity>();
		}
		else
		{
			throw new IllegalStateException("Batch was already in progress");
		}
	}

	@Override
	public void stopBatchMode()
	{
		if (batched == null)
		{
			throw new IllegalStateException("Batch was not in progress");
		}
		else if (!batched.isEmpty())
		{
			throw new IllegalStateException("Must flush batch before stopping");
		}
		else
		{
			batched = null;
		}
	}

	protected List<Key> flushEntities(Settings settings)
	{
		if (batched == null)
		{
			throw new IllegalStateException("Flush called but not in batch mode");
		}

		logger.info("Flush " + batched.size() + " entities");

		// put new entities
		List<Key> keys = bulkPutWithTransaction(Collections2.filter(batched.values(), Predicates.notNull()), settings);
		
		// delete removed entities
		bulkDeleteWithTransaction(Maps.filterValues(batched, Predicates.isNull()).keySet(), settings.getCacheMode());
		
		batched.clear();

		return keys;
	}

	protected final Key servicePut(Entity entity, Settings settings)
	{
		long start = System.currentTimeMillis();
		statistics.puts++;
		try
		{
			if (batched == null)
			{
				return singlePutWithTransaction(entity, settings);
			}
			else
			{
				batch(entity.getKey(), entity);

				// assume key is complete
				return entity.getKey();
			}
		}
		finally
		{
			if (logger.isLoggable(Level.FINE))
			{
				logger.fine(System.currentTimeMillis() - start + "ms " + entity.getKey().toString());
			}
		}
	}

	protected void batch(Key key, Entity entity)
	{
		batched.put(entity.getKey(), entity);
		if (batched.size() >= threshold)
		{
			flushEntities(defaultSettings);
		}
	}
	
	@Override
	public void setAutoflushThreshold(int threshold)
	{
		this.threshold = threshold;
	}

	private Key singlePutWithTransaction(Entity entity, Settings settings)
	{
		if (transaction == null || !transaction.isActive())
		{
			Key key = putToDatastoreWithRetry(entity, settings);
			CacheDetails details = kindToCache.get(entity.getKey().getKind());
			if (details != null)
			{
				// cache in both memory and memcache
				putToMemory(entity, details);
				putToMemcache(entity, details);
			}
			return key;
		}
		else
		{
			return service(settings).put(transaction, entity);
		}
	}

	private DatastoreService service(Settings settings)
	{
		DatastoreService service;
		if (settings != null)
		{
			service = newDatastoreService(settings);
		}
		else
		{
			service = defaultDatastoreService;
		}
		return service;
	}

	private boolean isCacheEnabled(CacheDetails details, CacheMode mode)
	{
		return details != null && (mode == CacheMode.ON || mode == CacheMode.AUTO && details.automatic);
	}

	private Key putToDatastoreWithRetry(Entity entity, Settings settings)
	{
		// re-try puts which is useful when using remote-api over bad connection
		RuntimeException last = null;
		int retries = settings == null ? defaultSettings.getRetries() : settings.getRetries();
		for (int tries = 0; tries < retries; tries++)
		{
			try
			{
				return service(settings).put(null, entity);
			}
			catch (RuntimeException e)
			{
				last = e;
				logger.log(Level.WARNING, "Problem during try " + tries, e);
			}
		}

		throw last;
	}

	// TODO look at caching entity protos instead
	private void putToMemcache(Entity result, CacheDetails details)
	{
		if (details.global)
		{
//			details.memcacheWrites.incrementAndGet();
			statistics.memcachePuts++;
			if (details.seconds >= 0)
			{
				getMemcacheService().put(datastoreToMemcacheKey(result.getKey()), result, Expiration.byDeltaSeconds(details.seconds));
			}
			else
			{
				getMemcacheService().put(datastoreToMemcacheKey(result.getKey()), result);
			}
		}
	}

	private AsyncMemcacheService getMemcacheService()
	{
		// multi-threaded but not important if more than one created
		if (memcache == null)
		{
			memcache = MemcacheServiceFactory.getAsyncMemcacheService();
		}
		return memcache;
	}

	/**
	 * Passing an Object to memcache requires serialization so we make
	 * a String key ourselves
	 */
	private String datastoreToMemcacheKey(Key key)
	{
		return MEMCACHE_PREFIX + KeyFactory.keyToString(key);
	}

	private Key memcacheToDatastoreKey(String key)
	{
		return KeyFactory.stringToKey(key.substring(MEMCACHE_PREFIX.length()));
	}

	protected final List<Key> servicePut(Collection<Entity> entities, Settings settings)
	{
		statistics.puts++;

		if (batched == null)
		{
			return bulkPutWithTransaction(entities, settings);
		}
		else
		{
			List<Key> keys = new ArrayList<Key>();
			for (Entity entity : entities)
			{
				batch(entity.getKey(), entity);
				keys.add(entity.getKey());
			}

			return keys;
		}
	}

	private List<Key> bulkPutWithTransaction(Collection<Entity> entities, Settings settings)
	{
		if (entities.isEmpty()) return Collections.emptyList();

		if (transaction == null || !transaction.isActive())
		{
			putToMemoryAndMemcache(entities, settings.getCacheMode());
			return putToDatastoreWithRetry(entities, settings);
		}
		else
		{
			return service(settings).put(transaction, entities);
		}
	}

	private List<Key> putToDatastoreWithRetry(Collection<Entity> entities, Settings settings)
	{
		RuntimeException last = null;
		int retries = defaultSettings.getRetries();
		for (int tries = 0; tries < retries; tries++)
		{
			try
			{
				return service(settings).put(null, entities);
			}
			catch (RuntimeException e)
			{
				last = e;
				logger.log(Level.WARNING, "Problem during try " + tries, e);
			}
		}

		throw last;
	}

	protected final Entity serviceGet(Key key, Settings settings) throws EntityNotFoundException
	{
		statistics.gets++;
		long start = System.currentTimeMillis();
		try
		{
			Entity result = null;
			if (transaction == null || !transaction.isActive())
			{
				CacheDetails details = kindToCache.get(key.getKind());
				if (isCacheEnabled(details, settings.getCacheMode()))
				{
					// look in the memory cache
					result = getFromMemory(key, details);

					// try in memcache
					if (result == null)
					{
						result = getFromMemcache(key, details);
						if (result != null)
						{
							putToMemory(result, details);
						}
						else
						{
							result = getFromDatastore(key, settings);
							if (result != null)
							{
								putToMemory(result, details);
								putToMemcache(result, details);
							}
						}
					}
					else
					{
//						details.memoryHits.incrementAndGet();
					}
//					details.reads.incrementAndGet();

				}
				else
				{
					result = getFromDatastore(key, settings);
				}
			}
			else
			{
				return getFromDatastore(key, settings);
			}
			return result;
		}
		finally
		{
			if (logger.isLoggable(Level.FINE))
			{
				logger.fine(System.currentTimeMillis() - start + "ms " + key.toString());
			}
		}
	}

	private Entity getFromDatastore(Key key, Settings settings) throws EntityNotFoundException
	{
		try
		{
			if (batched != null)
			{
				// allow for nulls which signify a deleted entity
				if (batched.containsKey(key))
				{
					return batched.get(key);
				}
			}
			
			statistics.datastoreGets++;
			Entity result;
			if (transaction == null || !transaction.isActive())
			{
				result = service(settings).get(null, key);
			}
			else
			{
				result = service(settings).get(transaction, key);
			}
			return result;
		}
		catch (EntityNotFoundException e)
		{
			// throwing an exception is silly
			return null;
		}
	}

	private void putToMemory(Entity result, CacheDetails details)
	{
		if (details.cache != null)
		{
			details.cache.put(result.getKey(), result);
		}
	}

	private Entity getFromMemory(Key key, CacheDetails details)
	{
		Entity result = null;
		if (details.cache != null)
		{
			result = details.cache.get(key);
		}

		return result;
	}

	private Entity getFromMemcache(Key key, CacheDetails details)
	{
		Entity result = null;
		if (details.global)
		{
			statistics.memcacheGets++;
			try
			{
				result = (Entity) getMemcacheService().get(datastoreToMemcacheKey(key)).get();
			}
			catch (Exception e)
			{
				if (e instanceof RuntimeException)
				{
					throw (RuntimeException) e;
				}
				else
				{
					throw new RuntimeException(e);
				}
			}
			
			if (result != null)
			{
				// only increment hits as total was ++ in memory cache
//				details.memcacheHits.incrementAndGet();
			}
		}

		return result;
	}

	protected final Map<Key, Entity> serviceGet(Collection<Key> keys, Settings settings)
	{
		statistics.gets++;
		long start = System.currentTimeMillis();
		try
		{
			if (transaction == null || !transaction.isActive())
			{
				Map<Key, Entity> fromMemory = getFromMemory(keys, settings.getCacheMode());

				Map<Key, Entity> result = null;
				if (!fromMemory.isEmpty())
				{
					keys = Collections2.filter(keys, not(in(fromMemory.keySet())));
					result = fromMemory;
				}

				if (keys.isEmpty())
				{
					return fromMemory;
				}

				Map<Key, Entity> fromMemcache = getFromMemcache(keys, settings.getCacheMode());

				// add all found in memcache back to the memory cache
				putToMemory(fromMemcache, settings.getCacheMode());

				if (!fromMemcache.isEmpty())
				{
					keys = Collections2.filter(keys, not(in(fromMemcache.keySet())));
					if (result == null)
					{
						result = fromMemcache;
					}
					else
					{
						result.putAll(fromMemcache);
					}
				}
				
				// check pending batched operations
				if (batched != null)
				{
					Iterator<Key> keyator = keys.iterator();
					while (keyator.hasNext())
					{
						Key key = (Key) keyator.next();
						
						// allow for null values which indicate a deleted entity
						if (batched.containsKey(key))
						{
							Entity entity = batched.get(key);
							
							// do not return anything for deleted entities
							if (result != null)
							{
								result.put(key, entity);
							}
							keyator.remove();
						}
					}
				}

				if (keys.isEmpty())
				{
					return result;
				}
				
				// get entities from the datastore
				statistics.datastoreGets++;
				Map<Key, Entity> fromDatastore = service(settings).get(null, keys);

				putToMemoryAndMemcache(fromDatastore.values(), settings.getCacheMode());

				if (!fromDatastore.isEmpty())
				{
					if (result == null)
					{
						result = fromDatastore;
					}
					else
					{
						result.putAll(fromDatastore);
					}
				}

				if (result == null)
				{
					return Collections.emptyMap();
				}
				else
				{
					return result;
				}
			}
			else
			{
				statistics.datastoreGets++;
				return service(settings).get(transaction, keys);
			}
		}
		finally
		{
			if (logger.isLoggable(Level.FINE))
			{
				logger.fine(System.currentTimeMillis() - start + "ms " + keys.toString());
			}
		}
	}

	private void putToMemory(Map<Key, Entity> fromMemcache, CacheMode mode)
	{
		for (Key key : fromMemcache.keySet())
		{
			CacheDetails details = kindToCache.get(key.getKind());
			if (isCacheEnabled(details, mode) && details.cache != null)
			{
				details.cache.put(key, fromMemcache.get(key));
			}
		}
	}

	// a combined method that does only one iteration of the entities
	protected void putToMemoryAndMemcache(Collection<Entity> entities, CacheMode mode)
	{
		// need to collect all entities with the same expiry time
		Map<Integer, Map<String, Entity>> secondsToKeyToEntity = null;

		// check each entity to see if we should cache it
		for (Entity entity : entities)
		{
			if (entity == null) throw new NullPointerException();

			CacheDetails details = kindToCache.get(entity.getKey().getKind());
			if (isCacheEnabled(details, mode))
			{
				// cache with memcache and memory
				if (details.cache != null)
				{
					details.cache.put(entity.getKey(), entity);
				}

				// we may not need memcache
				if (details.global)
				{
//					details.memcacheWrites.incrementAndGet();

					// put the entity in the memcache collection by expiry
					if (secondsToKeyToEntity == null)
					{
						secondsToKeyToEntity = Maps.newHashMap();
					}

					Map<String, Entity> keyToEntity = secondsToKeyToEntity.get(details.seconds);
					if (keyToEntity == null)
					{
						keyToEntity = Maps.newHashMapWithExpectedSize(entities.size());
						secondsToKeyToEntity.put(details.seconds, keyToEntity);
					}

					// make a string key to save serialization
					String stringKey = datastoreToMemcacheKey(entity.getKey());
					keyToEntity.put(stringKey, entity);
				}
			}
		}

		// this will be null if no entities are cached in memcache
		if (secondsToKeyToEntity != null)
		{
			// put in memcache each bunch of entities with the same expiry time
			for (Integer expiry : secondsToKeyToEntity.keySet())
			{
				statistics.memcachePuts++;
				Map<String, Entity> keyToEntity = secondsToKeyToEntity.get(expiry);
				if (expiry > 0)
				{
					getMemcacheService().putAll(keyToEntity, Expiration.byDeltaSeconds(expiry));
				}
				else
				{
					getMemcacheService().putAll(keyToEntity);
				}
			}
		}
	}

	private Map<Key, Entity> getFromMemory(Collection<Key> keys, CacheMode mode)
	{
		Map<Key, Entity> result = null;
		for (Key key : keys)
		{
			CacheDetails details = kindToCache.get(key.getKind());
			if (isCacheEnabled(details, mode))
			{
				// this is always called even when there is no memory cache
//				details.reads.incrementAndGet();

				Map<Key, Entity> cache = details.cache;

				if (cache != null)
				{
					// check the in-memory cache
					Entity cached = cache.get(key);
					if (cached != null)
					{
//						details.memoryHits.incrementAndGet();

						if (result == null)
						{
							result = new HashMap<Key, Entity>(keys.size());
						}
						result.put(key, cached);
					}
				}
			}
		}
		if (result == null)
		{
			return Collections.emptyMap();
		}
		else
		{
			return result;
		}
	}

	private Map<Key, Entity> getFromMemcache(Collection<Key> keys, CacheMode mode)
	{
		// convert keys to string keys used in memcache to avoid serialising
		Collection<String> stringKeys = null;
		for (Key key : keys)
		{
			// only look for cached entity kinds
			CacheDetails details = kindToCache.get(key.getKind());
			if (isCacheEnabled(details, mode) && details.global)
			{
				if (stringKeys == null)
				{
					stringKeys = new ArrayList<String>(keys.size());
				}
				stringKeys.add(datastoreToMemcacheKey(key));
			}
		}

		if (stringKeys == null)
		{
			return Collections.emptyMap();
		}

		// check memcache for entities
		statistics.memcacheGets++;
		Map<String, Object> cached;
		try
		{
			cached = getMemcacheService().getAll(stringKeys).get();
		}
		catch (Exception e)
		{
			if (e instanceof RuntimeException)
			{
				throw (RuntimeException) e;
			}
			else
			{
				throw new RuntimeException(e);
			}
		}

		// put each result in the memory cache
		Map<Key, Entity> result = new HashMap<Key, Entity>(cached.size());
		for (String stringKey : cached.keySet())
		{
			// convert back to datastore key to put in memory cache
			Key key = memcacheToDatastoreKey(stringKey);
			result.put(key, (Entity) cached.get(stringKey));

//			// all results are from cached kinds so details exist
//			CacheDetails details = kindToCache.get(key.getKind());
//
//			// only increment hits as total was ++ by memory cache
//			details.memcacheHits.addAndGet(cached.size());
		}

		return result;
	}

	protected final void serviceDelete(Collection<Key> keys, CacheMode mode)
	{
		if (batched == null)
		{
			bulkDeleteWithTransaction(keys, mode);
		}
		else
		{
			for (Key key : keys)
			{
				batch(key, null);
			}
		}
	}

	public void bulkDeleteWithTransaction(Collection<Key> keys, CacheMode mode)
	{
		if (transaction == null || !transaction.isActive())
		{
			Collection<String> removeFromMemcache = null;
			for (Key key : keys)
			{
				CacheDetails details = kindToCache.get(key.getKind());
				if (isCacheEnabled(details, mode))
				{
					if (details.cache != null)
					{
						details.cache.remove(key);
					}

					if (details.global)
					{
						if (removeFromMemcache == null)
						{
							removeFromMemcache = new ArrayList<String>(keys.size());
						}
						removeFromMemcache.add(datastoreToMemcacheKey(key));
					}
				}
			}

			if (removeFromMemcache != null)
			{
				getMemcacheService().deleteAll(removeFromMemcache);
			}

			defaultDatastoreService.delete(keys);
		}
		else
		{
			defaultDatastoreService.delete(transaction, keys);
		}
	}

	// TODO return an iterator that updates the cache
	protected final PreparedQuery servicePrepare(Query query, Settings settings)
	{
		if (transaction == null || !transaction.isActive())
		{
			return service(settings).prepare(null, query);
		}
		else
		{
			return service(settings).prepare(transaction, query);
		}
	}

	public DatastoreService getDefaultService()
	{
		return defaultDatastoreService;
	}

	public final Transaction getTransaction()
	{
		return transaction;
	}

	public final Transaction beginTransaction()
	{
		if (getTransaction() != null && getTransaction().isActive())
		{
			throw new IllegalStateException("Already in active transaction");
		}
		TransactionOptions options = TransactionOptions.Builder.withXG(defaultSettings.isCrossGroupTransactions());
		transaction = defaultDatastoreService.beginTransaction(options);
		return transaction;
	}

	public final Transaction beginOrJoinTransaction()
	{
		final Transaction current = getTransaction();
		if (current == null || !current.isActive())
		{
			return beginTransaction();
		}
		else
		{
			return new Transaction()
			{
				@Override
				public Future<Void> rollbackAsync()
				{
					return current.rollbackAsync();
				}
				
				@Override
				public void rollback()
				{
					current.rollback();
				}
				
				@Override
				public boolean isActive()
				{
					return current.isActive();
				}
				
				@Override
				public String getId()
				{
					return current.getId();
				}
				
				@Override
				public String getApp()
				{
					return current.getApp();
				}
				
				@Override
				public Future<Void> commitAsync()
				{
					return new AbstractFuture<Void>(){};
				}
				
				@Override
				public void commit()
				{
				}
			};
		}
	}

	@Override
	public void transact(final Runnable runnable)
	{
		transact(new Work<Void>()
		{
			@Override
			public Void perform(ObjectDatastore datastore)
			{
				runnable.run();
				return null;
			}
		});
	}
	
	@SuppressWarnings("unchecked")
	public final <T> T transact(Work<T> transactable)
	{
		Transaction transaction = beginOrJoinTransaction();
		try
		{
			Object result = transactable.perform(this);
			transaction.commit();
			return (T) result;
		}
		finally
		{
			if (transaction.isActive())
			{
				transaction.rollback();
			}
		}
	}
}
