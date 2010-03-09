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

	KeyFieldTranslator(StrategyObjectDatastore strategyObjectDatastore, PropertyTranslator chained, TypeConverter converters)
	{
		super(chained);
		this.datastore = strategyObjectDatastore;
		this.converters = converters;
	}

	public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
	{
		assert path.getParts().size() == 1 : "Key field should be in root Entity";

		// key spec may be null if we are in an update as we already have the key
		if (this.datastore.writeKeySpec != null)
		{
			if (instance != null)
			{
				// treat 0 the same as null
				if (!instance.equals(0))
				{
					// the key name is not stored in the fields but only in key
					if (Number.class.isAssignableFrom(instance.getClass()))
					{
						Long converted = converters.convert(instance, Long.class);
						this.datastore.writeKeySpec.setId(converted);
					}
					else
					{
						String keyName = converters.convert(instance, String.class);
						this.datastore.writeKeySpec.setName(keyName);
					}
				}
			}
		}
		return Collections.emptySet();
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		assert properties.isEmpty();

		// the key value is not stored in the properties but in the key
		Object keyValue = this.datastore.readKey.getName();
		if (keyValue == null)
		{
			keyValue = this.datastore.readKey.getId();
		}
		Object keyObject = converters.convert(keyValue, type);
		return keyObject;
	}
}