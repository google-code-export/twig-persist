package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;
import com.vercer.convert.BaseTypeConverter;
import com.vercer.convert.Converter;
import com.vercer.convert.ConverterRegistry;

public class ConverterTranslator implements PropertyTranslator
{
	private final Converter<?, ?> forward;
	private final Converter<?, ?> backward;

	private final Type source;
	private final Type target;

	public <S, T> ConverterTranslator(Converter<S, T> forward, Converter<T, S> backward)
	{
		this.forward = forward;
		this.backward = backward;
		source = BaseTypeConverter.sourceType(forward);
		target = BaseTypeConverter.targetType(forward);
	}

	public ConverterTranslator(Type source, Type target, ConverterRegistry converters)
	{
		this.source = source;
		this.target = target;
		this.forward = converters.converter(source, target);
		this.backward = converters.converter(target, source);
	}

	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		type = BaseTypeConverter.ignoreWildCards(type);
		if (type.equals(source))
		{
			if (properties.isEmpty()) return NULL_VALUE;

			Object value = PropertySets.firstValue(properties);
			if (value.getClass().equals(target))
			{
				return BaseTypeConverter.typesafe(backward, value);
			}
		}
		return null;
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		if (instance.getClass().equals(source))
		{
			Object value = BaseTypeConverter.typesafe(forward, instance);
			return PropertySets.singletonPropertySet(path, value, indexed);
		}
		return null;
	}
}
