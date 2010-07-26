package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;


/**
 * Determines if and how a field is stored in the datastore
 *
 * @author John Patterson <john@vercer.com>
 */
public interface StorageStrategy
{
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
}
