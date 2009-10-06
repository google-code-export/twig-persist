package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;

public abstract class InstanceTranslator implements PropertyTranslator
{
	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		return getInstance();
	}

	protected abstract Object getInstance();

	public Set<Property> typesafeToProperties(Object object, Path prefix, boolean indexed)
	{
		return Collections.singleton(getProperty());
	}

	protected abstract Property getProperty();

}
