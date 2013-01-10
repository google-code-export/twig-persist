package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;
import com.vercer.convert.CompositeTypeConverter;
import com.vercer.convert.TypeConverter;

public class ConverterTranslator implements PropertyTranslator
{
	private final TypeConverter forward;
	private final TypeConverter backward;

	private final Type source;
	private final Type target;

	public ConverterTranslator(Type source, Type target, CompositeTypeConverter converters)
	{
		this.source = source;
		this.target = target;
		this.forward = converters.converter(source, target);
		this.backward = converters.converter(target, source);
	}

	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		if (type.equals(source))
		{
			if (properties.isEmpty()) return NULL_VALUE;

			Object value = PropertySets.firstValue(properties);
			if (value == null) return null;
			
			if (value.getClass().equals(target))
			{
				return backward.convert(value, source);
			}
		}
		return null;
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		if (instance.getClass().equals(source))
		{
			Object value = forward.convert(instance, target);
			return PropertySets.singletonPropertySet(path, value, indexed);
		}
		return null;
	}
}
