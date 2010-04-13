/**
 * 
 */
package com.vercer.engine.persist;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

import com.google.appengine.api.datastore.Blob;
import com.vercer.engine.persist.annotation.Type;

public class TypeWithCollections
{
	public static class TypeWithEnum
	{
		public enum MyEnum { JOHN, WAS, HAIR, TWOK10 }; 
		MyEnum watsit;
	}
	
	@Type(Blob.class) Collection<Class<?>> classes = new ArrayList<Class<?>>();
	@Type(Blob.class) HashSet<TypeWithEnum> things = new HashSet<TypeWithEnum>();
}