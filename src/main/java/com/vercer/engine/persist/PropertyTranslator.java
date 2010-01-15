package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.Set;

public interface PropertyTranslator
{
	// TODO use SortedSet? Could have optimised array based version which asserts order
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type);
	public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed);
}
