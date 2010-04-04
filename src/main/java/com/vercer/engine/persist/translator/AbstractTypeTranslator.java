package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.engine.persist.util.SinglePropertySet;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;

public abstract class AbstractTypeTranslator<T> implements PropertyTranslator
{
	private final Class<T> clazz;

	public AbstractTypeTranslator(Class<T> clazz)
	{
		this.clazz = clazz;
	}

	protected abstract T decode(Object value);
	protected abstract Object encode(T value);

	public final Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		if (GenericTypeReflector.erase(type) == clazz)
		{
			return decode(PropertySets.firstValue(properties));
		}
		else
		{
			return null;
		}
	}

	@SuppressWarnings("unchecked")
	public final Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
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
