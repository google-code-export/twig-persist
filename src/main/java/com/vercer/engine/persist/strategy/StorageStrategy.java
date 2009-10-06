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
	 * @return
	 */
	boolean component(Field field);

	boolean entity(Field field);

	/**
	 * Should the field be included in the single field indexes
	 *
	 * Creates a property using Entity.setUnindexedProperty(String, Object)
	 *
	 * @param field The reflected Field to be examined
	 * @return true if the field is indexed
	 */
	boolean indexed(Field field);

	/**
	 * Should the field be stored at all
	 * @param field The reflected Field to be examined
	 * @return true to store the field
	 */
	boolean stored(Field field);


	boolean polyMorphic(Field field);
}
