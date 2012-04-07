package com.google.code.twig;

import java.lang.reflect.Type;
import java.util.Set;

public interface PropertyTranslator
{
	// TODO use a sorted set
	public Object decode(Set<Property> properties, Path path, Type type);
	public Set<Property> encode(Object instance, Path path, boolean indexed);
	public static Object NULL_VALUE = new Object();
}
