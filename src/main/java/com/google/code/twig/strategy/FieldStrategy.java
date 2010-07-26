package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public interface FieldStrategy
{
	/**
	 * @return The property name used for this field
	 */
	String name(Field field);
	
	
	/**
	 * @return The kind name used in the Entity that stores this type
	 */
	String typeToKind(Type type);
	
	
	/**
	 * @return The Type that is represented by this kind name in the Entity
	 */
	Type kindToType(String kind);
	
	
	/**
	 * @return The Type that field values should be converted to
	 */
	Type typeOf(Field field);
}
