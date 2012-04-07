package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.vercer.convert.TypeConverter;

public class TypedTranslator extends DecoratingTranslator
{
	private final Type type;
	private final TypeConverter converter;

	public TypedTranslator(PropertyTranslator chained, Type type, TypeConverter converter)
	{
		super(chained);
		this.type = type;
		this.converter = converter;
	}

	@Override
	public Object decode(Set<Property> properties, Path path, Type ignore)
	{
		return chained.decode(properties, path, type);
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		Object converted = converter.convert(instance, type);
		return chained.encode(converted, path, indexed);
	}

}
