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
	private final StrategyObjectDatastore strategyObjectDatastore;
	private final TypeConverter converters;

	KeyFieldTranslator(StrategyObjectDatastore strategyObjectDatastore, PropertyTranslator chained, TypeConverter converters)
	{
		super(chained);
		this.strategyObjectDatastore = strategyObjectDatastore;
		this.converters = converters;
	}

	public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
	{
		assert path.getParts().size() == 1 : "Key field should be in root Entity";

		// key spec may be null if we are in an update as we already have the key
		if (this.strategyObjectDatastore.writeKeySpec != null)
		{
			if (instance != null)
			{
				// treat 0 the same as null
				if (!instance.equals(0))
				{
					// the key name is not stored in the fields but only in key
					String keyName = converters.convert(instance, String.class);
					this.strategyObjectDatastore.writeKeySpec.setName(keyName);
				}
			}
		}
		return Collections.emptySet();
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		assert properties.isEmpty();

		// the key value is not stored in the properties but in the key
		Object keyValue = this.strategyObjectDatastore.readKey.getName();
		if (keyValue == null)
		{
			keyValue = this.strategyObjectDatastore.readKey.getId();
		}
		Object keyObject = converters.convert(keyValue, type);
		return keyObject;
	}
}