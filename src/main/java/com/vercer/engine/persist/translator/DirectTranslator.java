package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.SimpleProperty;

public class DirectTranslator implements PropertyTranslator
{
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		if (isDirectType(type))
		{
			Property property = properties.iterator().next();
			if (property == null)
			{
				return null;
			}
			else
			{
				return property.getValue();
			}
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
			return Collections.singleton((Property) new SimpleProperty(path, object, indexed));
		}
		else
		{
			return null;
		}
	}
}
