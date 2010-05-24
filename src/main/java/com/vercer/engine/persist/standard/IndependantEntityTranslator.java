/**
 * 
 */
package com.vercer.engine.persist.standard;

import java.lang.reflect.Type;
import java.util.Set;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.engine.persist.util.SinglePropertySet;
import com.vercer.util.reference.ReadOnlyObjectReference;

final class IndependantEntityTranslator implements PropertyTranslator
{
	private final StrategyObjectDatastore strategyObjectDatastore;
	private static final Logger logger = Logger.getLogger(IndependantEntityTranslator.class.getName());

	/**
	 * @param strategyObjectDatastore
	 */
	IndependantEntityTranslator(StrategyObjectDatastore strategyObjectDatastore)
	{
		this.strategyObjectDatastore = strategyObjectDatastore;
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		Key key = PropertySets.firstValue(properties);
		Object result = this.strategyObjectDatastore.getInstanceFromCacheOrLoad(key);
		if (result == null)
		{
			result = NULL_VALUE;
			logger.warning("No entity found for referenced key " + key);
		}
		return result;
	}

	public Set<Property> typesafeToProperties(final Object instance, final Path path, final boolean indexed)
	{
		assert instance != null;
		ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
		{
			public Key get()
			{
				// clear the current key spec so it is not used as parent
				KeySpecification current = IndependantEntityTranslator.this.strategyObjectDatastore.writeKeySpec;
				IndependantEntityTranslator.this.strategyObjectDatastore.writeKeySpec = null;

				Key key = IndependantEntityTranslator.this.strategyObjectDatastore.getKeyFromCacheOrStore(instance);

				// replace it to continue processing potential children
				IndependantEntityTranslator.this.strategyObjectDatastore.writeKeySpec = current;

				return key;
			}
		};

		return new SinglePropertySet(path, keyReference, indexed);
	}
}