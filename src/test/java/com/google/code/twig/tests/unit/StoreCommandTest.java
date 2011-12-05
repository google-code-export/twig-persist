package com.google.code.twig.tests.unit;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Child;
import com.google.code.twig.annotation.Entity;
import com.google.code.twig.annotation.GaeKey;
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
	
	static class HasGaeKey
	{
		@GaeKey String key;
		int value;
		public HasGaeKey()
		{
		}
		public HasGaeKey(int value)
		{
			super();
			this.value = value;
		}
	}
	
	@Test public void storeSetsId()
	{
		HasGaeKey model = new HasGaeKey(4);
		
		Key key = datastore.store(model);
	
		Assert.assertNotNull(model.key);
	
		datastore.disassociateAll();
		
		HasGaeKey reloaded = datastore.load(key);

		Assert.assertEquals(reloaded.key, model.key);
		
		reloaded.value = 3;
		
		datastore.update(reloaded);

		Assert.assertEquals(reloaded.key, model.key);
	}
	
}
