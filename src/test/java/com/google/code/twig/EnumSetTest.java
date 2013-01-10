package com.google.code.twig;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class EnumSetTest extends LocalDatastoreTestCase
{
	public EnumSetTest()
	{
		ObjectDatastoreFactory.register(EnumContainer.class);
	}
	
	public enum MyEnum { HELLO, THERE };
	
	public static class EnumContainer 
	{
		EnumSet<MyEnum> theEnumSet = EnumSet.noneOf(MyEnum.class);
	}
	
	@Test
	public void testStoreLoadEnumSet()
	{
		// TODO removed enum conversion temporarily
		
		EnumSet<MyEnum> myEnums = EnumSet.allOf(MyEnum.class);
		EnumContainer container = new EnumContainer();
		container.theEnumSet = myEnums;
		
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		Key key = datastore.store(container);
		
		datastore.disassociateAll();
		
		EnumContainer loaded = datastore.load(key);
		
		Assert.assertTrue(loaded.theEnumSet instanceof EnumSet<?>);
		Assert.assertTrue(loaded.theEnumSet.size() == 2);
	}
}
