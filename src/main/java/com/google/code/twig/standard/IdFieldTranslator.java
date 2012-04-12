/**
 *
 */
package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.translator.DecoratingTranslator;

final class IdFieldTranslator extends DecoratingTranslator
{
	private final TranslatorObjectDatastore datastore;

	IdFieldTranslator(TranslatorObjectDatastore datastore, PropertyTranslator chained)
	{
		super(chained);
		this.datastore = datastore;
	}

	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		// key specification may be null if we are in an update as we already have the key
		if (datastore.encodeKeySpec != null)
		{
			if (instance != null)
			{
				// the key name is not stored in the fields but only in key
				if (instance instanceof Number)
				{
					// only set the id if it is not 0 otherwise auto-generate
					long longValue = ((Number) instance).longValue();
					if (longValue != 0l)
					{
						datastore.encodeKeySpec.setId(longValue);
					}
				}
				else if (instance instanceof String)
				{
					datastore.encodeKeySpec.setName((String) instance);
				}
			}
		}
		return Collections.emptySet();
	}

	public Object decode(Set<Property> properties, Path prefix, Type type)
	{
		// the key value is not stored in the properties but in the key
		Object id = datastore.decodeKey.getName();
		if (id == null)
		{
			id = datastore.decodeKey.getId();
		}
		
		id = datastore.getTypeConverter().convert(id, type);
		
		return id;
	}
}