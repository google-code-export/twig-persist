package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.SimpleProperty;
import com.google.code.twig.util.generic.Generics;

public class EnumTranslator implements PropertyTranslator
{

	@SuppressWarnings("unchecked")
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		Class<?> clazz = Generics.erase(type);
		if (clazz.isEnum())
		{
			Property property = properties.iterator().next();
			String name = (String) property.getValue();
			@SuppressWarnings("rawtypes")
			Class<? extends Enum> ce = (Class<? extends Enum>) clazz;
			return Enum.valueOf(ce, name);
		}
		else
		{
			return null;
		}
	}

	public Set<Property> encode(Object object, Path path, boolean indexed)
	{
		if (object instanceof Enum<?>)
		{
			String name = ((Enum<?>) object).name();
			Property property = new SimpleProperty(path, name, indexed);
			return Collections.singleton(property);
		}
		else
		{
			return null;
		}
	}

}
