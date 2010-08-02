package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.conversion.CombinedTypeConverter;
import com.google.code.twig.conversion.SpecificTypeConverter;
import com.google.code.twig.util.PropertySets;

public class ConvertableTypeTranslator implements PropertyTranslator
{
	private final CombinedTypeConverter converter;

	public ConvertableTypeTranslator(CombinedTypeConverter converter)
	{
		this.converter = converter;
	}
	
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		if (properties.size() == 1)
		{
			Object value = PropertySets.firstValue(properties);
			if (value instanceof String || value instanceof Long)
			{
				return typesafeConvert(type, value);
			}
			else if (value == null)
			{
				return NULL_VALUE;
			}
		}
		else if (properties.isEmpty())
		{
			return NULL_VALUE;
		}
		return null;
	}

	private <S, T> T typesafeConvert(Type type, S value)
	{
		@SuppressWarnings("unchecked")
		SpecificTypeConverter<S, T> specific = (SpecificTypeConverter<S, T>) converter.converter(value.getClass(), type);
		if (specific != null)
		{
			return specific.convert(value);
		}
		else
		{
			return null;
		}
	}

	public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
	{
		Object value = typesafeConvert(String.class, instance);
		if (value == null)
		{
			value = typesafeConvert(Long.class, instance);
		}
		
		if (value != null)
		{
			return PropertySets.singletonPropertySet(path, value, indexed);
		}
		else
		{
			return null;
		}
	}
}
