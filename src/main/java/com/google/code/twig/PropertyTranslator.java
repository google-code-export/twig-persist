package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.Set;

public interface PropertyTranslator
{
	// TODO both should have parameters (Object, Type, Set<Property>, Path):void
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type);
	public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed);

	// TODO make a special value for could-not-handle instead
	public static Object NULL_VALUE = new Object();
	
	// TODO should have instantiate method like GWT generator?
	// Object instantiate(Type, Set<Property>)
}
