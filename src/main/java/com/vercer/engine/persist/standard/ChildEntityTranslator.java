/**
 *
 */
package com.vercer.engine.persist.standard;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.engine.persist.util.SinglePropertySet;
import com.vercer.util.reference.ReadOnlyObjectReference;

final class ChildEntityTranslator implements PropertyTranslator
{
	private final StrategyObjectDatastore strategyObjectDatastore;

	/**
	 * @param strategyObjectDatastore
	 */
	ChildEntityTranslator(StrategyObjectDatastore strategyObjectDatastore)
	{
		this.strategyObjectDatastore = strategyObjectDatastore;
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		Key key = PropertySets.firstValue(properties);
		return this.strategyObjectDatastore.keyToInstance(key, null);
	}

	public Set<Property> typesafeToProperties(final Object instance, final Path path, final boolean indexed)
	{
		ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
		{
			public Key get()
			{
				// clear the current key spec so it is not used as parent
				KeySpecification current = ChildEntityTranslator.this.strategyObjectDatastore.writeKeySpec;

				Key key = ChildEntityTranslator.this.strategyObjectDatastore.getKeyCache().getCachedKey(instance);
				if (key == null)
				{
					key = ChildEntityTranslator.this.strategyObjectDatastore.store(instance);
				}

				// replace it to continue processing potential children
				ChildEntityTranslator.this.strategyObjectDatastore.writeKeySpec = current;

				return key;
			}
		};

		return new SinglePropertySet(path, keyReference, indexed);
	}
}