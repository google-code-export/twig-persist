package com.google.code.twig.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public interface Configuration
{

	int activationDepth(Field field, int depth);
	

	/**
	 * @return The kind name used in the Entity that stores this type
	 */
	String typeToKind(Type type);
	
	
	/**
	 * @return The Type that is represented by this kind name in the Entity
	 */
	Type kindToType(String kind);
	
	/**
	 * @param type The type for which we need a unique id
	 * @return A unique id 
	 */
	long allocateIdsFor(Type type);

	/**
	 * The key field value will be converted to a String and become the name 
	 * of the key which will not be stored as a property. The value must not
	 * be null and must be convertible to and from a String unambiguously.
	 * 
	 * @param field The reflected Field to examine
	 * @return <code>true</code> if the field holds a unique key for this type 
	 */
	boolean id(Field field);
	
	/**
	 * @param field The reflected Field to be examined
	 * @return
	 */
	boolean parent(Field field);
	
	/**
	 * @param field The reflected Field to be examined
	 * @return
	 */
	boolean child(Field field);
	/**
	 * @param field The reflected Field to be examined
	 * @return true to store as a component of the referencing Entity
	 */
	boolean embed(Field field);

	/**
	 * Should this field value be stored as a separate Entity
	 * @param field
	 * @return true to store as a datastore Entity
	 */
	boolean entity(Field field);

	/**
	 * Should the field be included in the single field indexes
	 *
	 * Creates a property using Entity.setUnindexedProperty(String, Object)
	 *
	 * @param field The reflected Field to be examined
	 * @return true if the field is indexed
	 */
	boolean index(Field field);

	/**
	 * Should the field be stored at all
	 * @param field The reflected Field to be examined
	 * @return true to store the field
	 */
	boolean store(Field field);


	/**
	 * Can this field hold sub-types of the declared field type
	 * @param field
	 * @return true if sub-types are allowed
	 */
	boolean polymorphic(Field field);
	

	/**
	 * @return The Type that field values should be converted to
	 */
	Type typeOf(Field field);
	

	/**
	 * @return The property name used for this field
	 */
	String name(Field field);
	
}
