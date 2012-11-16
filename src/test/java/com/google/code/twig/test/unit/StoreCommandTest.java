package com.google.code.twig.test.unit;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.ObjectDatastoreFactory;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Child;
import com.google.code.twig.annotation.Entity;
import com.google.code.twig.annotation.Id;

public class StoreCommandTest extends LocalDatastoreTestCase
{
	private ObjectDatastore datastore;

	public StoreCommandTest()
	{
		ObjectDatastoreFactory.register(ChildClass.class);
		ObjectDatastoreFactory.register(ParentClass.class);
	}
	
	@Before
	public void setup()
	{
		datastore = new AnnotationObjectDatastore();
	}
	
	@Entity(kind="pc", allocateIdsBy=10)
	public static class ParentClass
	{
		@Id long id;
		@Child ChildClass child;
		public ParentClass(ChildClass child)
		{
			this.child = child;
		}
	}
	
	public static class ChildClass
	{
		@Id long id;
		String name;
		public ChildClass(String string)
		{
			this.name = string;
		}
	}
	
	@Test
	public void allocateIdsToParent()
	{
		// without allocating the ancestors id this would fail
		ChildClass child = new ChildClass("TheChild");
		ParentClass parent = new ParentClass(child);
		
		datastore.store(parent);
	}
	
	@Test 
	public void batchStoreAndUpdate()
	{
		datastore.startBatchMode();
		
		ChildClass child1 = new ChildClass("hello");
		ChildClass child2 = new ChildClass("there");
		
		datastore.store(child1);
		datastore.store(child2);
		
		Assert.assertEquals(0, child1.id);
		
		datastore.flushBatchedOperations();

		Assert.assertTrue(child1.id != 0);
	}
	
}
