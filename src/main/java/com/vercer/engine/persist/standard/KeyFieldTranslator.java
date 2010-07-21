/**
 *
 */
package com.vercer.engine.persist.standard;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.conversion.TypeConverter;
import com.vercer.engine.persist.translator.DecoratingTranslator;

final class KeyFieldTranslator extends DecoratingTranslator
{
	private final StrategyObjectDatastore datastore;
	private final TypeConverter converters;

	KeyFieldTranslator(StrategyObjectDatastore datastore, PropertyTranslator chained, TypeConverter converters)
	{
		super(chained);
		this.datastore = datastore;
		this.converters = converters;
	}

	public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
	{
		assert path.getParts().size() == 1 : "Key field must be in root Entity";

		// key spec may be null if we are in an update as we already have the key
		if (datastore.encodeKeySpec != null)
		{
			if (instance != null)
			{
				// the key name is not stored in the fields but only in key
				if (Number.class.isAssignableFrom(instance.getClass()))
				{
					// only set the id if it is not 0 otherwise auto-generate
					long longValue = ((Number) instance).longValue();
					if (longValue != 0l)
					{
						datastore.encodeKeySpec.setId(longValue);
					}
				}
				else
				{
					String keyName = converters.convert(instance, String.class);
					datastore.encodeKeySpec.setId(keyName);
				}
			}
		}
		return Collections.emptySet();
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		assert properties.isEmpty();

		// the key value is not stored in the properties but in the key
		Object keyValue = datastore.decodeKey.getName();
		if (keyValue == null)
		{
			keyValue = datastore.decodeKey.getId();
		}
		return converters.convert(keyValue, type);
	}
}