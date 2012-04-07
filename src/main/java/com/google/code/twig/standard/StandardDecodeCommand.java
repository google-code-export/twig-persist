package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.code.twig.LoadCommand.CacheMode;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.Restriction;
import com.google.code.twig.Settings;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.RestrictionToPredicateAdaptor;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

@SuppressWarnings("unchecked")
class StandardDecodeCommand<C extends StandardDecodeCommand<C>> extends StandardCommand
{
//	private static final Logger log = Logger.getLogger(StandardDecodeCommand.class.getName());
	protected Integer activationDepth;
	protected Restriction<Entity> entityRestriction;
	protected Restriction<Property> propertyRestriction;
	protected CacheMode cacheMode;
	private Settings.Builder settings;
	
	StandardDecodeCommand(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}
	
	public C restrictEntities(Restriction<Entity> restriction)
	{
		this.entityRestriction = restriction;
		return (C) this;
	}

	public C restrictProperties(Restriction<Property> restriction)
	{
		this.propertyRestriction = restriction;
		return (C) this;
	}
	
	public C cache(CacheMode cacheMode)
	{
		this.cacheMode = cacheMode;
		return (C) this;
	}
	
	public final <T> T entityToInstance(Entity entity, Restriction<Property> predicate)
	{
		long start = System.currentTimeMillis();
		
		// we have the entity data but must return the associated instance
		T instance = (T) datastore.keyCache.getInstance(entity.getKey());
		
		// if the instance is unactivated and we fetched the data then use it 
		
		Iterator<?> existingRefreshInstances =  datastore.refreshInstances;
		if (instance != null && datastore.activationDepth >= 0 && !datastore.isActivated(instance))
		{
			// do not create new instance - reuse this one
			datastore.refreshInstances = ImmutableList.of(instance).iterator();
		
			// just to trigger the activation code
			instance = null;
		}
		
		if (instance == null)
		{
			// push new decode context state
			Key existingDecodeKey = datastore.decodeKey;
			datastore.decodeKey = entity.getKey();

			try
			{
				Type type = datastore.getConfiguration().kindToType(entity.getKind());
	
				Set<Property> properties = PropertySets.create(entity.getProperties(), datastore.indexed);
				
				// filter out unwanted properties at this low level
				if (predicate != null)
				{
					properties = Sets.filter(properties, new RestrictionToPredicateAdaptor<Property>(predicate));
				}
	
				// order the properties for efficient separation by field
				Set<Property> sorted = new TreeSet<Property>();
				sorted.addAll(properties);
				properties = sorted;
	
				instance = (T) datastore.decoder(entity).decode(properties, Path.EMPTY_PATH, type);
				
				// null signifies that the properties could not be decoded
				if (instance == null)
				{
					throw new IllegalStateException("Could not translate entity " + entity);
				}
				
				// a null value is indicated by this special return value
				if (instance == PropertyTranslator.NULL_VALUE)
				{
					instance = null;
				}
				
				// set if this instance is activated or not
				datastore.keyCache.setActivation(instance, datastore.activationDepth >= 0);
			}
			finally
			{
				// pop the decode context
				datastore.decodeKey = existingDecodeKey;
				datastore.refreshInstances = existingRefreshInstances;
			}
		}

		return instance;
	}
	

	public final <T> Iterator<T> entitiesToInstances(final Iterator<Entity> entities, final Restriction<Property> filter)
	{
		return new Iterator<T>()
		{
			@Override
			public boolean hasNext()
			{
				return entities.hasNext();
			}

			@Override
			public T next()
			{
				return (T) entityToInstance(entities.next(), filter);
			}

			@Override
			public void remove()
			{
				entities.remove();
			}
		};
	}

	// get from key cache or datastore
	public <T> T keyToInstance(Key key, Restriction<Property> filter)
	{
		int existingActivationDepth = datastore.activationDepth;
		if (activationDepth != null)
		{
			datastore.activationDepth = activationDepth;
		}
		
		try
		{
			T instance = (T) datastore.keyCache.getInstance(key);
			if (instance == null)
			{
				Entity entity = keyToEntity(key);
				if (entity == null)
				{
					instance = null;
				}
				else
				{
					instance = (T) entityToInstance(entity, filter);
				}
			}
	
			return instance;
		}
		finally
		{
			datastore.activationDepth = existingActivationDepth;
		}
	}
	
	public final <T> Map<Key, T> keysToInstances(Collection<Key> keys, Restriction<Property> filter)
	{
		int existingActivationDepth = datastore.activationDepth;
		if (activationDepth != null)
		{
			datastore.activationDepth = activationDepth;
		}
		try
		{
			Map<Key, T> result = new HashMap<Key, T>(keys.size());
			List<Key> missing = null;
			for (Key key : keys)
			{
				T instance = (T) datastore.keyCache.getInstance(key);

				// if we are doing a refresh/associate then do load the entity
				if (instance != null && datastore.refreshInstances == null)
				{
					result.put(key, instance);
				}
				else
				{
					if (missing == null)
					{
						missing = new ArrayList<Key>(keys.size());
					}
					missing.add(key);
				}
			}
			
			if (missing != null && !missing.isEmpty())
			{
				Map<Key, Entity> entities = keysToEntities(missing);
				
				// must decode in same order as keys - needed for refreshing
				for (Key key : missing)
				{
					Entity entity = entities.get(key);

					if (entity == null) continue;
					
					// must disassociate here so instance is not loaded
					// other instances that reference it will still work
					if (datastore.refreshInstances != null)
					{
						datastore.keyCache.evictKey(key);
					}
					
					T instance = (T) entityToInstance(entity, filter);

					result.put(key, instance);
				}
			}
	
			return result;
		}
		finally
		{
			datastore.activationDepth = existingActivationDepth;
		}
	}

	final Entity keyToEntity(Key key)
	{
		if (datastore.activationDepth >= 0)
		{
			CacheMode existingCacheMode = datastore.getCacheMode();
			if (cacheMode != null)
			{
				datastore.setCachMode(cacheMode);
			}
			
			try
			{
				return datastore.serviceGet(key, getSettings());
			}
			catch (EntityNotFoundException e)
			{
				return null;
			}
			finally
			{
				datastore.setCachMode(existingCacheMode);
			}
		}
		else
		{
			// don't load entity if it will not be activated - but need one for key
			return new Entity(key);
		}
	}
	
	final Map<Key, Entity> keysToEntities(Collection<Key> keys)
	{
		CacheMode existingCacheMode = datastore.getCacheMode();
		if (cacheMode != null)
		{
			datastore.setCachMode(cacheMode);
		}
		
		// only load entity if we will activate instance
		if (datastore.activationDepth >= 0)
		{
			try
			{
				return datastore.serviceGet(keys, getSettings());
				
			}
			finally
			{
				datastore.setCachMode(existingCacheMode);
			}
		}
		else
		{
			// we must return empty entities with the correct kind to instantiate
			HashMap<Key, Entity> result = new HashMap<Key, Entity>();
			for (Key key : keys)
			{
				result.put(key, new Entity(key));
			}
			return result;
		}
	}

	public C activate(int depth)
	{
		this.activationDepth = depth;
		return (C) this;
	}

	public C activateAll()
	{
		this.activationDepth = Integer.MAX_VALUE;
		return (C) this;
	}
	
	public C unactivated()
	{
		// do not activate the current instance - 0 means no referenced activated
		activate(-1);
		return (C) this;
	}

	public C consistency(Consistency consistency)
	{
		getSettingsBuilder().consistency(consistency);
		return (C) this;
	}

	public C deadline(long deadline, TimeUnit unit)
	{
		getSettingsBuilder().deadline(deadline, unit);
		return (C) this;
	}
	
	public Settings.Builder getSettingsBuilder()
	{
		if (settings == null)
		{
			settings = Settings.copy(datastore.getDefaultSettings());
		}
		return this.settings;
	}
	
	public Settings getSettings()
	{
		if (settings == null)
		{
			return null;
		}
		else
		{
			return settings.build();
		}
	}
}
