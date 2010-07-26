package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;

public interface RelationshipStrategy
{
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
	 * The key field value will be converted to a String and become the name 
	 * of the key which will not be stored as a property. The value must not
	 * be null and must be convertible to and from a String unambiguously.
	 * 
	 * @param field The reflected Field to examine
	 * @return <code>true</code> if the field holds a unique key for this type 
	 */
	boolean key(Field field);
}
