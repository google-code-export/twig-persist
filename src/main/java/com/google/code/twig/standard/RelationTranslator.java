/**
 * 
 */
package com.google.code.twig.standard;

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
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.SinglePropertySet;
import com.google.code.twig.util.reference.ObjectReference;
import com.google.code.twig.util.reference.ReadOnlyObjectReference;

class RelationTranslator implements PropertyTranslator
{
	protected final TranslatorObjectDatastore datastore;
	private static final Logger logger = Logger.getLogger(RelationTranslator.class.getName());

	/**
	 * @param strategyObjectDatastore
	 */
	RelationTranslator(TranslatorObjectDatastore strategyObjectDatastore)
	{
		this.datastore = strategyObjectDatastore;
	}

	public Object decode(Set<Property> properties, Path prefix, Type type)
	{
		if (properties.isEmpty() || properties.size() == 1 && PropertySets.firstValue(properties) == null)
		{
			return NULL_VALUE;
		}
		
		Object value = PropertySets.firstValue(properties);
		if (value instanceof Collection<?>)
		{
			@SuppressWarnings("unchecked")
			List<Key> keys = (List<Key>) value;
			return keysToInstances(keys);
		}
		else
		{
			return keyToInstance((Key) value);
		}
	}

	protected Collection<Object> keysToInstances(List<Key> keys)
	{
		Map<Key, Object> keysToInstances;
		try
		{
			datastore.activationDepth--;
			keysToInstances = datastore.load().keys(keys).returnResultsNow();
		}
		finally
		{
			datastore.activationDepth++;
		}
		
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

	protected Object keyToInstance(Key key)
	{
		Object result;
		try
		{
			datastore.activationDepth--;
			result = this.datastore.load().key(key).now();
		}
		finally
		{
			datastore.activationDepth++;
		}
		
		if (result == null)
		{
			result = NULL_VALUE;
			logger.warning("No entity found for referenced key " + key);
		}
		return result;
	}

	public Set<Property> encode(final Object instance, final Path path, final boolean indexed)
	{
		if (instance == null)
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
					Collection<?> instances = (Collection<?>) instance;
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
					return instanceToKey(instance);
				}
			};
		}

		return new SinglePropertySet(path, item, indexed);
	}
	
	protected Key instanceToKey(final Object instance)
	{
		Key key = datastore.associatedKey(instance);
		if (key == null)
		{
			key = datastore.store().instance(instance).parentKey(getParentKey()).now();
		}
		return key;
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
			result.putAll(datastore.store().instances(missed).parentKey(parentKey).now());
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