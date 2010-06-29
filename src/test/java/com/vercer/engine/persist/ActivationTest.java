package com.vercer.engine.persist;

import java.io.Serializable;
import java.util.List;

import junit.framework.Assert;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.common.collect.Lists;
import com.vercer.engine.persist.annotation.Activate;
import com.vercer.engine.persist.annotation.AnnotationObjectDatastore;

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

	static class C implements Serializable
	{
		private static final long serialVersionUID = 1L;
		String field;
	}

	@Test
	public void noActivationEmbedded()
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
		
		Assert.assertNull(reloaded.bs.get(0).cs.get(0).field);
	}
}
