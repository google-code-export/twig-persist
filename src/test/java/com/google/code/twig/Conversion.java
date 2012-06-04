package com.google.code.twig;

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
		Registry registry = new Registry(new Annotator());
		
		StandardObjectDatastore storeWith = new StandardObjectDatastore(Settings.builder().build(), new AnnotationConfiguration()
		{
			@Override
			protected String typeToName(Class<?> type)
			{
				return "name";
			}
		}, 0, false);
		
		Key key = storeWith.store(new ClassWithInteger());
		
		StandardObjectDatastore loadWith = new StandardObjectDatastore(Settings.builder().build(), new AnnotationConfiguration()
		{
			@Override
			protected Class<?> nameToType(String name)
			{
				return ClassWithString.class;
			}
		}, 0, false);
		
		ClassWithString loaded = loadWith.load(key);
		
		assert loaded.value == "9";
	}
}
