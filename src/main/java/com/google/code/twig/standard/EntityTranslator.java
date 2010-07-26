/**
 * 
 */
package com.vercer.engine.persist.standard;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.engine.persist.util.SinglePropertySet;
import com.vercer.util.reference.ObjectReference;
import com.vercer.util.reference.ReadOnlyObjectReference;

class EntityTranslator implements PropertyTranslator
{
	protected final StrategyObjectDatastore datastore;
	private static final Logger logger = Logger.getLogger(EntityTranslator.class.getName());

	/**
	 * @param strategyObjectDatastore
	 */
	EntityTranslator(StrategyObjectDatastore strategyObjectDatastore)
	{
		this.datastore = strategyObjectDatastore;
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		if (properties.isEmpty())
		{
			return NULL_VALUE;
		}
		
		Object value = PropertySets.firstValue(properties);
		if (value instanceof Collection<?>)
		{
			@SuppressWarnings("unchecked")
			List<Key> keys = (List<Key>) value;
			Map<Key, Object> keysToInstances = datastore.load().keys(keys).returnResultsNow();
			List<Object> result = new ArrayList<Object>();
			
			// keep order the same as keys
			Iterator<Key> iterator = keys.iterator();
			while(iterator.hasNext())
			{
				Key key = iterator.next();
				Object instance = keysToInstances.get(key);
				if (instance != null)
				{
					result.add(instance);
					iterator.remove();
				}
				else
				{
					logger.warning("No entity found for referenced key " + key);
				}
			}
			return result;
		}
		else
		{
			Key key = (Key) value;
			Object result = this.datastore.load().key(key).returnResultNow();
			if (result == null)
			{
				result = NULL_VALUE;
				logger.warning("No entity found for referenced key " + key);
			}
			return result;
		}
	}

	public Set<Property> typesafeToProperties(final Object instance, final Path path, final boolean indexed)
	{
		if (instance== null)
		{
			return Collections.emptySet();
		}
		
		ObjectReference<?> item;
		if (instance instanceof Collection<?>)
		{
			item = new ReadOnlyObjectReference<List<Key>>()
			{
				@Override
				public List<Key> get()
				{
					// get keys for each item
					List<?> instances = (List<?>) instance;
					Map<?, Key> instancesToKeys = instancesToKeys(instances, getParentKey());
					
					// need to make sure keys are in same order as original instances
					List<Key> keys = new ArrayList<Key>(instances.size());
					for (Object instance : instances)
					{
						keys.add(instancesToKeys.get(instance));
					}
					
					return keys;
				}
			};
		}
		else
		{
			// delay creating actual entity until all current fields have been encoded
			item = new ReadOnlyObjectReference<Key>()
			{
				public Key get()
				{
					Key key = datastore.associatedKey(instance);
					if (key == null)
					{
						key = datastore.store().instance(instance).parentKey(getParentKey()).returnKeyNow();
					}
					return key;
				}
			};
		}

		return new SinglePropertySet(path, item, indexed);
	}
	
	private <T> Map<T, Key> instancesToKeys(Collection<T> instances, Key parentKey)
	{
		Map<T, Key> result = new HashMap<T, Key>(instances.size());
		List<T> missed = new ArrayList<T>(instances.size());
		for (T instance : instances)
		{
			Key key = datastore.associatedKey(instance);
			if (key == null)
			{
				missed.add(instance);
			}
			else
			{
				result.put(instance, key);
			}
		}
		
		if (!missed.isEmpty())
		{
			// encode the instances to entities
			result.putAll(datastore.store().instances(missed).parentKey(parentKey).returnKeysNow());
		}
		
		return result;
	}


	/**
	 * Override to create parent child relationships
	 */
	protected Key getParentKey()
	{
		return null;
	}
}