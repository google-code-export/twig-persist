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
	private final StrategyObjectDatastore datastore;

	/**
	 * @param datastore
	 */
	ParentEntityTranslator(StrategyObjectDatastore datastore)
	{
		this.datastore = datastore;
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		// properties are not used as the parent is found by the key
		assert properties.isEmpty();

		// put the key in a property
		Key parentKey = datastore.decodeKey.getParent();

		if (parentKey == null)
		{
			throw new IllegalStateException("No parent for key: " + datastore.decodeKey);
		}

		return this.datastore.load().key(parentKey).returnResultNow();
	}

	public Set<Property> typesafeToProperties(final Object instance, final Path prefix, final boolean indexed)
	{
		ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
		{
			public Key get()
			{
				return instanceToKey(instance);
			}
		};

		// an existing parent key ref shows parent is still being stored
		if (datastore.encodeKeySpec != null && datastore.encodeKeySpec.getParentKeyReference() == null)
		{
			// store the parent key inside the current key
			datastore.encodeKeySpec.setParentKeyReference(keyReference);
		}

		// no fields are stored for parent
		return Collections.emptySet();
	}

	protected Key instanceToKey(Object instance)
	{
		Key key = datastore.associatedKey(instance);
		if (key == null)
		{
			key = datastore.store().instance(instance).returnKeyNow();
		}
		return key;
	}
}