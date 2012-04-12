package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.vercer.convert.TypeConverter;

public class TypeConverterTranslator extends DecoratingTranslator
{
	private final TypeConverter converter;

	public TypeConverterTranslator(PropertyTranslator chained, TypeConverter converter)
	{
		super(chained);
		this.converter = converter;
	}

	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		return null;
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		return null;
	}

}
