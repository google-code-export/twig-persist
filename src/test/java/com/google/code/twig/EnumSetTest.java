package com.google.code.twig;

import java.util.EnumSet;

import org.junit.Assert;
import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class EnumSetTest extends LocalDatastoreTestCase
{
	public enum MyEnum { HELLO, THERE };
	
	public static class EnumContainer 
	{
		EnumSet<MyEnum> theEnumSet;
	}
	
	@Test
	public void testStoreLoadEnumSet()
	{
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
