package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;

public class EnumTranslator implements PropertyTranslator
{

	@SuppressWarnings("unchecked")
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		Class<?> clazz = GenericTypeReflector.erase(type);
		if (clazz.isEnum())
		{
			Property property = properties.iterator().next();
			String name = (String) property.getValue();
			Class<? extends Enum> ce = (Class<? extends Enum>) clazz;
			return Enum.valueOf(ce, name);
		}
		else
		{
			return null;
		}
	}

	public Set<Property> typesafeToProperties(Object object, Path path, boolean indexed)
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
