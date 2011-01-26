package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.Restriction;
import com.google.code.twig.util.PropertyComparator;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.RestrictionToPredicateAdaptor;
import com.google.common.collect.Sets;

class StandardDecodeCommand extends StandardCommand
{
	private static final PropertyComparator COMPARATOR = new PropertyComparator();
	
	StandardDecodeCommand(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}
	
	@SuppressWarnings("unchecked")
	public final <T> T entityToInstance(Entity entity, Restriction<Property> predicate)
	{
		// we have the entity data but must return the associated instance
		T instance = (T) datastore.keyCache.getInstance(entity.getKey());
		if (instance == null)
		{
			// push new decode context state
			Key existingDecodeKey = datastore.decodeKey;
			datastore.decodeKey = entity.getKey();

			try
			{
				Type type = datastore.getConfiguration().kindToType(entity.getKind());
	
				Set<Property> properties = PropertySets.create(entity.getProperties(), datastore.indexed);
				
				// filter out unwanted properties at the lowest level
				if (predicate != null)
				{
					properties = Sets.filter(properties, new RestrictionToPredicateAdaptor<Property>(predicate));
				}
	
				// order the properties for efficient separation by field
				Set<Property> sorted = new TreeSet<Property>(COMPARATOR);
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
			}
			finally
			{
				// pop the decode context
				datastore.decodeKey = existingDecodeKey;
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

			@SuppressWarnings("unchecked")
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
	@SuppressWarnings("unchecked")
	public <T> T keyToInstance(Key key, Restriction<Property> filter)
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
	
	@SuppressWarnings("unchecked")
	public final <T> Map<Key, T> keysToInstances(Collection<Key> keys, Restriction<Property> filter)
	{
		Map<Key, T> result = new HashMap<Key, T>(keys.size());
		List<Key> missing = null;
		for (Key key : keys)
		{
			T instance = (T) datastore.keyCache.getInstance(key);
			if (instance != null)
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
			if (!entities.isEmpty())
			{
				Set<Entry<Key, Entity>> entries = entities.entrySet();
				for (Entry<Key, Entity> entry : entries)
				{
					T instance = (T) entityToInstance(entry.getValue(), filter);
					result.put(entry.getKey(), instance);
				}
			}
		}

		return result;
	}

	final Entity keyToEntity(Key key)
	{
		if (datastore.activationDepth >= 0)
		{
			try
			{
				return datastore.serviceGet(key);
			}
			catch (EntityNotFoundException e)
			{
				return null;
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
		// only load entity if we will activate instance
		if (datastore.activationDepth >= 0)
		{
			return datastore.serviceGet(keys);
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
}
