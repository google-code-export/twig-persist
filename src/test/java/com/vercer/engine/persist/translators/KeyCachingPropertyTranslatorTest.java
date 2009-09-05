package com.vercer.engine.persist.translators;

import static org.junit.Assert.assertTrue;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.repackaged.com.google.common.collect.BiMap;
import com.google.appengine.repackaged.com.google.common.collect.HashBiMap;
import com.vercer.engine.LocalServiceTestCase;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.translator.KeyCachingTranslator;
import com.vercer.engine.persist.util.SimpleProperty;


public class KeyCachingPropertyTranslatorTest extends LocalServiceTestCase
{
	@Test
	public void cachingTest()
	{
		// create a translator that always creates a new key and always creates a new object result
		PropertyTranslator chained = new PropertyTranslator()
		{
			public Set<Property> typesafeToProperties(Object object, Path path, boolean indexed)
			{
				return Collections.singleton((Property) new SimpleProperty(path, KeyFactory.createKey("test", System.currentTimeMillis()), indexed));
			}
			
			public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
			{
				return new Object();
			}
		};
		
		BiMap<Key, Object> cache = HashBiMap.create();
		KeyCachingTranslator translator = new KeyCachingTranslator(chained);
		
		// create an object to store
		Object object = new Object();

		// encode the same object twice
		Set<Property> encoded1 = translator.typesafeToProperties(object, Path.EMPTY_PATH, true);
		Set<Property> encoded2 = translator.typesafeToProperties(object, Path.EMPTY_PATH, true);
		
		// get the keys out
		Key key1 = (Key) encoded1.iterator().next().getValue();
		Key key2 = (Key) encoded2.iterator().next().getValue();
		
		// the keys must be the same instance
		assertTrue(key1 == key2);
		
		Object decoded = translator.propertiesToTypesafe(encoded2, Path.EMPTY_PATH, Object.class);
		
		// the same instance should be returned
		assertTrue(decoded == object);  
		
	}
}
