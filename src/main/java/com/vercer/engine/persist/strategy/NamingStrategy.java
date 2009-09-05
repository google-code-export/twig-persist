package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public interface NamingStrategy
{
	String name(Field field);
	String typeToKind(Type type);
	Type kindToType(String kind);
}
