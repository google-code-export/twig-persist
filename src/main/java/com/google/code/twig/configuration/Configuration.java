package com.google.code.twig.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.google.code.twig.ObjectDatastore;

public interface Configuration
{
	/**
	 * <p>Activation depth controls how many referenced instances will be loaded 
	 * from the datastore. When a referenced instance is loaded the current
	 * activation depth is decremented and used as the new activation depth
	 * when activating this referenced instance.</p>
	 * 
	 * <p>When the activation depth reaches 0 for an instance, no entities will
	 * be fetched from the datastore to populate its referenced instances.  
	 * Instead any referenced instances will be "unactivated" which means
	 * their fields will not be set and will remain default values.</p>
	 * 
	 * <p>An unactivated instance can be activated using {@link ObjectDatastore#activate(Object...)}
	 * when its values are required</p>
	 * 
	 * @param field The field which contains the instance being decoded
	 * @param depth The current activation depth
	 * @return The new activation depth for decoding this instance
	 */
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
	 * Returning a value greater than 0 will cause that number of unique ids to
	 * be reserved at a time and cached in this ObjectDatastore. This is useful 
	 * if you need to store a reference to an instance of this type before it is 
	 * stored and you have not defined an id field.
	 * 
	 * @param type The type which may need a unique id allocated
	 * @return The number of ids to allocate at a time
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
	 * Can this field hold GAE Keys?
	 * @param field
	 * @return true if the field holds a GAE key
	 */
	boolean gaeKey(Field field);
	

	/**
	 * @return The Type that field values should be converted to
	 */
	Type typeOf(Field field);
	

	/**
	 * @return The property name used for this field
	 */
	String name(Field field);
	
}
