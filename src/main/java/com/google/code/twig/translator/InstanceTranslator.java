package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;

public abstract class InstanceTranslator implements PropertyTranslator
{
	public Object decode(Set<Property> properties, Path prefix, Type type)
	{
		return getInstance();
	}

	protected abstract Object getInstance();

	public Set<Property> encode(Object object, Path prefix, boolean indexed)
	{
		return Collections.singleton(getProperty());
	}

	protected abstract Property getProperty();

}
