package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;

public class DirectTranslator implements PropertyTranslator
{
	public Object decode(Set<Property> properties, Path path, Type type)
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

	public Set<Property> encode(Object object, Path path, boolean indexed)
	{
		if (object == null || isDirectType(object.getClass()))
		{
			return PropertySets.singletonPropertySet(path, object, indexed);
		}
		else
		{
			return null;
		}
	}
}
