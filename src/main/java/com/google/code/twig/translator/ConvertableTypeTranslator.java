package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.conversion.CombinedConverter;
import com.google.code.twig.conversion.SpecificConverter;
import com.google.code.twig.util.PropertySets;

/**
 * Translates any type that can be converted into a String or Long by the
 * supplied TypeConverter.  Note that the type should be convertible in both
 * directions or an encoded value might not be able to be decoded again.
 * 
 * TODO try to convert to more native types
 * 
 * @author John Patterson <john@vercer.com>
 */
public class ConvertableTypeTranslator implements PropertyTranslator
{
	private final CombinedConverter converter;

	public ConvertableTypeTranslator(CombinedConverter converter)
	{
		this.converter = converter;
	}
	
	public Object decode(Set<Property> properties, Path path, Type type)
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
		SpecificConverter<S, T> specific = (SpecificConverter<S, T>) converter.converter(value.getClass(), type);
		if (specific != null)
		{
			return specific.convert(value);
		}
		else
		{
			return null;
		}
	}

	public Set<Property> encode(Object instance, Path path, boolean indexed)
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
