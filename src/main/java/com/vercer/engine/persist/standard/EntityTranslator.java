/**
 * 
 */
package com.vercer.engine.persist.standard;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
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
			Map<Key, Object> keysToInstances = this.datastore.keysToInstances(keys, null);
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
			Object result = this.datastore.keyToInstance(key, null);
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
					Map<?, Key> instancesToKeys = datastore.instancesToKeys(instances, getParentKey());
					
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
					return datastore.instanceToKey(instance, getParentKey());
				}
			};
		}

		return new SinglePropertySet(path, item, indexed);
	}

	protected Key getParentKey()
	{
		return null;
	}
}