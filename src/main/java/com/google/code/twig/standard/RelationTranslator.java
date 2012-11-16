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

public class RelationTranslator implements PropertyTranslator
{
	protected final TranslatorObjectDatastore datastore;
	private static final Logger logger = Logger.getLogger(RelationTranslator.class.getName());
	
	/**
	 * @param strategyObjectDatastore
	 */
	public RelationTranslator(TranslatorObjectDatastore strategyObjectDatastore)
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
			if (properties.isEmpty()) return createCollection();

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
	
	protected Collection<Object> createCollection()
	{
		// support reusing existing implementations 
		if (datastore.refresh != null)
		{
			@SuppressWarnings("unchecked")
			Collection<Object> result = (Collection<Object>) datastore.refresh;
			datastore.refresh = null;
			return result;
		}
		else
		{
			return new ArrayList<Object>();
		}
	}

	protected Collection<Object> keysToInstances(List<Key> keys)
	{
		// use the same settings as the current decode command
		StandardDecodeCommand<?> current = (StandardDecodeCommand<?>) datastore.command;
		
		// create a new command which replaces the current command
		StandardUntypedMultipleLoadCommand load = datastore.load().keys(keys);
		
		// keep the same settings as the current command
		transferCommandState(current, load);
		
		// create or reuse collection before create children
		Collection<Object> result = createCollection();

		// load the instances
		Map<Key, Object> keysToInstances = load.now();

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
		
		// replace current command
		datastore.command = current;
		
		return result;
	}

	protected Object keyToInstance(Key key)
	{
		// use the same settings as the current decode command
		StandardDecodeCommand<?> current = (StandardDecodeCommand<?>) datastore.command;
		
		// create a new command which replaces the current command
		StandardUntypedSingleLoadCommand load = datastore.load().key(key);
	
		// keep the same settings as the current command
		transferCommandState(current, load);
		
		// get the instance by key
		Object result = load.now();
		
		// replace the current command
		datastore.command = current;
		
		if (result == null)
		{
			// ignore missing instances as they might have been deleted
			result = NULL_VALUE;
			logger.warning("No entity found for referenced key " + key);
		}
		return result;
	}

	private void transferCommandState(StandardDecodeCommand<?> current, StandardDecodeCommand<?> decode)
	{
		decode.activate(current.getDepth() - 1);
		decode.builder = current.builder;
	}

	public Set<Property> encode(final Object instance, final Path path, final boolean indexed)
	{
		if (instance == null)
		{
			return PropertySets.singletonPropertySet(path, null, indexed);
		}
		else if (datastore.associating && !isKeyRelation())
		{
			return Collections.emptySet();
		}

		ObjectReference<?> reference;
		if (instance instanceof Collection<?>)
		{
			final Collection<?> instances = (Collection<?>) instance;

			if (instances.isEmpty()) return Collections.emptySet();

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

	protected boolean isKeyRelation()
	{
		return false;
	}

	protected Key instanceToKey(final Object instance)
	{
		StandardCommonStoreCommand<?, ?> existing = (StandardCommonStoreCommand<?, ?>) datastore.command;
		
		Key key = datastore.associatedKey(instance);
		if (key == null || !key.isComplete() || existing.command.cascade)
		{
			key = datastore.store()
					.cascade(existing.command.cascade)
					.instance(instance)
					.parentKey(getParentKey())
					.now();
		}

//		if (!key.isComplete())
//		{
//			throw new IllegalStateException("Incomplete key for instance " + instance);
//		}
		
		datastore.command = existing;

		return key;
	}

	private <T> Map<T, Key> instancesToKeys(Collection<T> instances, Key parentKey)
	{		
		StandardCommonStoreCommand<?, ?> existing = (StandardCommonStoreCommand<?, ?>) datastore.command;

		Map<T, Key> result = new IdentityHashMap<T, Key>(instances.size());
		List<T> missed = new ArrayList<T>(instances.size());
		for (T instance : instances)
		{
			Key key = datastore.associatedKey(instance);
			if (key == null || existing.command.cascade)
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
			 Map<T, Key> instanceToKey = datastore.store()
					.cascade(existing.command.cascade)
					.instances(missed)
					.parentKey(getParentKey())
					.now();
			 
			result.putAll(instanceToKey);
		}

		for (Key key : result.values())
		{
			if (!key.isComplete())
			{
				throw new IllegalStateException("Incomplete key  " + key);
			}
		}

		datastore.command = existing;
		
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