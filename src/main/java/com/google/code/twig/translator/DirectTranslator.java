package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.conversion.PrimitiveConverter;
import com.google.code.twig.conversion.TypeConverter;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.generic.Generics;

public class DirectTranslator implements PropertyTranslator
{
	private final TypeConverter converter;

	public DirectTranslator(TypeConverter converter)
	{
		this.converter = converter;
	}
	
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		if (isDirectType(type))
		{
			Object value;
			if (properties.isEmpty())
			{
				return NULL_VALUE;
			}
			else
			{
				value = PropertySets.firstValue(properties);
				if (value == null && properties.size() == 1)
				{
					Class<?> clazz = Generics.erase(type);
					if (clazz.isPrimitive())
					{
						Class<?> wrapper = PrimitiveConverter.getWrapperClassForPrimitive(clazz);
						return PrimitiveConverter.defaultPrimitiveValue(wrapper);
					}
					else
					{
						return NULL_VALUE;
					}
				}
			}
			
			if (type instanceof Class<?> && 
					(value.getClass() == type || 
					PrimitiveConverter.getWrapperClassForPrimitive((Class<?>) type) == value.getClass()))
			{
				return value;
			}
			else
			{
				return converter.convert(value, type);
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
