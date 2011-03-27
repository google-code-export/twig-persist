/**
 *
 */
package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.util.reference.ReadOnlyObjectReference;

final class ParentRelationTranslator extends RelationTranslator
{
	/**
	 * @param datastore
	 */
	ParentRelationTranslator(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}

	public Object decode(Set<Property> properties, Path prefix, Type type)
	{
		// properties are not used as the parent is found by the key
		assert properties.isEmpty();

		// put the key in a property
		Key parentKey = datastore.decodeKey.getParent();

		if (parentKey == null)
		{
			return NULL_VALUE;
		}

		return keyToInstance(parentKey);
	}

	public Set<Property> encode(final Object instance, final Path prefix, final boolean indexed)
	{
		if (instance != null)
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
		}

		// no fields are stored for parent
		return Collections.emptySet();
	}
}