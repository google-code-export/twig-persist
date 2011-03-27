package com.google.code.twig.tests.unit;

import org.junit.Before;
import org.junit.Test;

import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Child;
import com.google.code.twig.annotation.Entity;
import com.google.code.twig.annotation.Id;

public class StoreCommandTest extends LocalDatastoreTestCase
{
	private AnnotationObjectDatastore datastore;

	@Before
	public void setup()
	{
		datastore = new AnnotationObjectDatastore();
	}
	
	@Entity(allocateIdsBy=10)
	public static class AllocateParent
	{
		@Id long id;
		@Child AllocateChild child;
		public AllocateParent(AllocateChild child)
		{
			this.child = child;
		}
	}
	
	public static class AllocateChild
	{
		String name;
		public AllocateChild(String string)
		{
			this.name = string;
		}
	}
	
	@Test
	public void allocateIdsToParent()
	{
		AllocateChild child = new AllocateChild("TheChild");
		AllocateParent parent = new AllocateParent(child);
		
		datastore.store(parent);
	}

}
