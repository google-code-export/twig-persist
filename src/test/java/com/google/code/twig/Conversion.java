package com.google.code.twig;

import java.lang.reflect.Type;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.annotation.AnnotationConfiguration;
import com.google.code.twig.standard.StandardObjectDatastore;

public class Conversion extends LocalDatastoreTestCase
{
	private static class ClassWithInteger
	{
		@SuppressWarnings("unused")
		int value = 9;
	}
	
	private static class ClassWithString
	{
		String value;
	}
	
	@Test
	public void storeAndLoadWithDifferentFieldType()
	{
		StandardObjectDatastore storeWith = new StandardObjectDatastore(new AnnotationConfiguration(true)
		{
			@Override
			protected String typeToName(Type type)
			{
				return "name";
			}
		});
		
		Key key = storeWith.store(new ClassWithInteger());
		
		StandardObjectDatastore loadWith = new StandardObjectDatastore(new AnnotationConfiguration(true)
		{
			@Override
			protected Type nameToType(String name)
			{
				return ClassWithString.class;
			}
		});
		
		ClassWithString loaded = loadWith.load(key);
		
		assert loaded.value == "9";
	}
}
