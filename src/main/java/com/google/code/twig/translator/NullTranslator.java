package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;

/**
 * Encodes a null value as an single item property set with the value null.
 * 
 * Decodes a single value property set with the value null as NULL_VALUE
 * 
 * Returns null in all other situations.
 * 
 * @author John Patterson (jdpatterson@gmail.com)
 */
public class NullTranslator implements PropertyTranslator
{
	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		if (properties.size() == 1)
		{
			Property property = PropertySets.firstProperty(properties);
			if (property.getPath().equals(path) && property.getValue() == null)
			{
				return NULL_VALUE;
			}
		}
		return null;
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		if (instance == null) 
		{
			return PropertySets.singletonPropertySet(path, null, indexed);
		}
		else 
		{
			return null;
		}
	}
}
