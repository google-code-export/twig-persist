/**
 * 
 */
package com.vercer.engine.persist;

import java.util.ArrayList;
import java.util.Collection;

import com.google.appengine.api.datastore.Blob;
import com.vercer.engine.persist.annotation.Type;

public class TypeWithCollection
{
	@Type(Blob.class) Collection<Class<?>> classes = new ArrayList<Class<?>>();
}