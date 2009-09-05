package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.Set;

public interface PropertyTranslator
{
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type);
	public Set<Property> typesafeToProperties(Object object, Path path, boolean indexed);
}
