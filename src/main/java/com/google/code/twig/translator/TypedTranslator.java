package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;

public class TypedTranslator extends DecoratingTranslator
{
	private final Type type;

	public TypedTranslator(PropertyTranslator chained, Type type)
	{
		super(chained);
		this.type = type;
	}

	@Override
	public Object decode(Set<Property> properties, Path path, Type ignore)
	{
		return chained.decode(properties, path, type);
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		return chained.encode(instance, path, indexed);
	}

}
