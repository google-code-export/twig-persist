/**
/**
 *
 */
package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
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
import com.google.code.twig.util.generic.Generics;
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
		if (properties.size() == 1 && PropertySets.firstValue(properties) == null)
		{
			return NULL_VALUE;
		}

		if (Collection.class.isAssignableFrom(Generics.erase(type)))
		{
			if (properties.isEmpty()) return new ArrayList<Object>();

			Object value = PropertySets.firstValue(properties);

			@SuppressWarnings("unchecked")
			List<Key> keys = (List<Key>) value;
			return keysToInstances(keys);
		}
		else
		{
			if (properties.isEmpty()) return NULL_VALUE;

			Object value = PropertySets.firstValue(properties);
			return keyToInstance((Key) value);
		}
	}

	protected List<Object> keysToInstances(List<Key> keys)
	{
		// use the same settings as the current decode command
		StandardDecodeCommand<?> current = (StandardDecodeCommand<?>) datastore.command;
		StandardUntypedMultipleLoadCommand load = datastore.load().keys(keys);
		transferCurrentCommandState(current, load);
		
		Map<Key, Object> keysToInstances = load.now();
		List<Object> result = new ArrayList<Object>();

		// keep order the same as keys
		for (Key key : keys)
		{
			Object instance = keysToInstances.get(key);
			if (instance != null)
			{
				result.add(instance);
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
		// use the same settings as the current decode command
		StandardDecodeCommand<?> current = (StandardDecodeCommand<?>) datastore.command;
		StandardUntypedSingleLoadCommand load = this.datastore.load().key(key);
		transferCurrentCommandState(current, load);
		
		// get the instance by key
		Object result = load.now();
		
		if (result == null)
		{
			result = NULL_VALUE;
			logger.warning("No entity found for referenced key " + key);
		}
		return result;
	}

	private void transferCurrentCommandState(StandardDecodeCommand<?> current, StandardDecodeCommand<?> decode)
	{
		decode.currentActivationDepth = current.currentActivationDepth;
		decode.builder = current.builder;
	}

	public Set<Property> encode(final Object instance, final Path path, final boolean indexed)
	{
		if (instance == null)
		{
			return PropertySets.singletonPropertySet(path, null, indexed);
		}

		ObjectReference<?> reference;
		if (instance instanceof Collection<?>)
		{
			final Collection<?> instances = (Collection<?>) instance;

			if (instances.isEmpty()) return Collections.emptySet();

			// if we are associating check that all referenced instances are already associated
			if (datastore.associating)
			{
				for (Object element : instances)
				{
					if (!datastore.isAssociated(element))
					{
						throw new IllegalStateException("Referenced instance " + element + " was not associated.");
					}
				}
			}

			reference = new ReadOnlyObjectReference<List<Key>>()
			{
				@Override
				public List<Key> get()
				{
					// get keys for each item
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
			if (datastore.associating && !datastore.isAssociated(instance))
			{
				throw new IllegalStateException("Referenced instance " + instance + " was not associated.");
			}

			// delay creating actual entity until all current fields have been encoded
			reference = new ReadOnlyObjectReference<Key>()
			{
				public Key get()
				{
					return instanceToKey(instance);
				}
			};
		}

		return new SinglePropertySet(path, reference, indexed);
	}

	protected Key instanceToKey(final Object instance)
	{
		Key key = datastore.associatedKey(instance);
		if (key == null || !key.isComplete())
		{
			key = datastore.store().instance(instance).parentKey(getParentKey()).now();
		}

//		if (!key.isComplete())
//		{
//			throw new IllegalStateException("Incomplete key for instance " + instance);
//		}

		return key;
	}

	private <T> Map<T, Key> instancesToKeys(Collection<T> instances, Key parentKey)
	{
		Map<T, Key> result = new IdentityHashMap<T, Key>(instances.size());
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

		for (Key key : result.values())
		{
			if (!key.isComplete())
			{
				throw new IllegalStateException("Incomplete key  " + key);
			}
		}

		return result;
	}


	/**
	 * Override to create ancestors child relationships
	 */
	protected Key getParentKey()
	{
		return null;
	}
}