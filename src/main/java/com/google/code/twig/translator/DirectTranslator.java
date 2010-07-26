package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.engine.persist.util.SinglePropertySet;

public class DirectTranslator implements PropertyTranslator
{
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		if (isDirectType(type))
		{
			if (properties.isEmpty())
			{
				return NULL_VALUE;
			}
			return PropertySets.firstValue(properties);
		}
		else
		{
			return null;
		}
	}

	protected boolean isDirectType(Type type)
	{
		return true;
	}

	public Set<Property> typesafeToProperties(Object object, Path path, boolean indexed)
	{
		if (isDirectType(object.getClass()))
		{
			return new SinglePropertySet(path, object, indexed);
		}
		else
		{
			return null;
		}
	}
}
