package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.SinglePropertySet;
import com.google.code.twig.util.generic.Generics;

public abstract class AbstractTypeTranslator<T> implements PropertyTranslator
{
	private final Class<T> clazz;

	public AbstractTypeTranslator(Class<T> clazz)
	{
		this.clazz = clazz;
	}

	protected abstract T decode(Object value);
	protected abstract Object encode(T value);

	public final Object decode(Set<Property> properties, Path path, Type type)
	{
		if (Generics.erase(type) == clazz)
		{
			return decode(PropertySets.firstValue(properties));
		}
		else
		{
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public final Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		if (clazz.isInstance(instance))
		{
			return new SinglePropertySet(path, encode((T) instance), indexed);
		}
		else
		{
			return null;
		}
	}
}
