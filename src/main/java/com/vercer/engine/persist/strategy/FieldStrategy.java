package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public interface FieldStrategy
{
	String name(Field field);
	String typeToKind(Type type);
	Type kindToType(String kind);
	Type typeOf(Field field);
}
