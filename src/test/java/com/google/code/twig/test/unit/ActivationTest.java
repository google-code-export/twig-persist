package com.google.code.twig.test.unit;

import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.Activate;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Id;
import com.google.common.collect.Lists;

public class ActivationTest extends LocalDatastoreTestCase
{
	static class A
	{
		List<B> bs;
	}

	static class B
	{
		@Activate(0)
		List<C> cs;
	}

	static class C
	{
		@Id long id;
		String field;
	}

	@Test
	public void activationIsLimitedByAnnotation()
	{
		A a = new A();
		B b1 = new B();
		B b2 = new B();
		a.bs = Lists.newArrayList(b1, b2);
		C c11 = new C();
		c11.field = "c11";
		C c12 = new C();
		c12.field = "c12";
		b1.cs = Lists.newArrayList(c11, c12);
		
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		Key key = datastore.store(a);
		datastore.disassociateAll();
		
		A reloaded = datastore.load(key);
		
		// show c is not activated
		Assert.assertNull(reloaded.bs.get(0).cs.get(0).field);
		
		// show that the id was set
		Assert.assertTrue(reloaded.bs.get(0).cs.get(0).id > 0);
		
		// now activate in bulk
		datastore.activateAll(reloaded.bs.get(0).cs);
		
		// show c is now activated
		Assert.assertNotNull(reloaded.bs.get(0).cs.get(0).field);
	}
	
	static class X
	{
		Y y;
	}
	
	static class Y
	{
		@Activate
		String field;
	}
	
	@Test
	public void overrideActivationDepth()
	{
		ObjectDatastore datastore = new AnnotationObjectDatastore();
		datastore.setActivationDepth(1);
		
		X x = new X();
		x.y = new Y();
		x.y.field = "hi";
		
		Key key = datastore.store(x);
		
		datastore.disassociateAll();
		
		X reloaded = datastore.load(key);
		
		Assert.assertNotNull(reloaded.y.field);
	}
}
