package com.google.code.twig.tests.issues;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.junit.Test;

import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Embedded;
import com.google.code.twig.annotation.Id;

public class Issue66Test extends LocalDatastoreTestCase
{
	static class Bag 
	{
		  @Id Long id;
		  @Embedded(polymorphic = true)
		  HashMap<String, Object> attrs = new HashMap<String, Object>();
	}
	
	static class Stuff 
	{
		String name = "j-lo";
		String asset = "ass";
	}

	@Test
	public void mixedPrimitiveAndObjectMap()
	{
		AnnotationObjectDatastore datastore = new AnnotationObjectDatastore();
		Bag bag = new Bag();
		bag.attrs.put("bar", new Date());
		bag.attrs.put("num", 8);
		bag.attrs.put("stuff", new Stuff());
		
		datastore.store(bag);

		datastore.disassociateAll();
		
		List<Bag> bags = datastore.find().type(Bag.class).returnAll().now();
		
		assert bags.size() == 1;
		
	}
}
