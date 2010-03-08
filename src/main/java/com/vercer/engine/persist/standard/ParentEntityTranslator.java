/**
 *
 */
package com.vercer.engine.persist.standard;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.util.reference.ReadOnlyObjectReference;

final class ParentEntityTranslator implements PropertyTranslator
{
	private final StrategyObjectDatastore strategyObjectDatastore;

	/**
	 * @param strategyObjectDatastore
	 */
	ParentEntityTranslator(StrategyObjectDatastore strategyObjectDatastore)
	{
		this.strategyObjectDatastore = strategyObjectDatastore;
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		// properties are not used as the parent is found by the key
		assert properties.isEmpty();

		// put the key in a property
		Key parentKey = this.strategyObjectDatastore.readKey.getParent();

		if (parentKey == null)
		{
			throw new IllegalStateException("No parent for key: " + this.strategyObjectDatastore.readKey);
		}

		return this.strategyObjectDatastore.getInstanceFromCacheOrLoad(parentKey);
	}

	public Set<Property> typesafeToProperties(final Object instance, final Path prefix, final boolean indexed)
	{
		// the parent key is not stored as properties but inside the key
		ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
		{
			public Key get()
			{
				// clear the current key spec so the parent is not also child
				KeySpecification current = ParentEntityTranslator.this.strategyObjectDatastore.writeKeySpec;
				ParentEntityTranslator.this.strategyObjectDatastore.writeKeySpec = null;

				Key key = ParentEntityTranslator.this.strategyObjectDatastore.getKeyFromCacheOrStore(instance);

				ParentEntityTranslator.this.strategyObjectDatastore.writeKeySpec = current;

				return key;
			}
		};

		// an existing parent key ref shows parent is still being stored
		if (this.strategyObjectDatastore.writeKeySpec != null && this.strategyObjectDatastore.writeKeySpec.getParentKeyReference() == null)
		{
			// store the parent key inside the current key
			this.strategyObjectDatastore.writeKeySpec.setParentKeyReference(keyReference);
		}

		// no fields are stored for parent
		return Collections.emptySet();
	}
}