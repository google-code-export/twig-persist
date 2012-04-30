package com.google.code.twig.test.unit;

import java.util.Date;
import java.util.Iterator;

import junit.framework.Assert;

import org.junit.Test;

import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.ObjectDatastoreFactory;
import com.google.code.twig.annotation.Id;
import com.google.code.twig.annotation.Parent;
import com.google.common.collect.Iterators;

public class DescendantsTest extends LocalDatastoreTestCase
{
	public static class Granddad
	{
		@Id
		String name;

		Granddad()
		{
		}
		
		public Granddad(String name)
		{
			this.name = name;
		}
	}
	
	public static class Mum
	{
		int kids;
		@Parent Granddad pops;
		
		Mum()
		{
		}

		public Mum(Granddad pops, int kids)
		{
			this.pops = pops;
			this.kids = kids;
		}
	}
	
	public static class Child
	{
		Date born;
		@Parent Mum mum;
		
		Child()
		{
		}
		
		public Child(Mum mum, Date born)
		{
			this.mum = mum;
			this.born = born;
		}
	}
	
	@Test
	public void storeLoadDescendants()
	{
		Granddad granddad = new Granddad("pops");
		Mum mum = new Mum(granddad, 8);
		@SuppressWarnings("deprecation")
		Child child = new Child(mum, new Date(1998, 8, 20));
		
		ObjectDatastore datastore = ObjectDatastoreFactory.createObjectDatastore();
		datastore.store(child);
		
		datastore = ObjectDatastoreFactory.createObjectDatastore();
		Granddad pops = datastore.load(Granddad.class, "pops");

		assert pops != granddad;
		
		Iterator<Object> descendants = datastore.find().descendants(granddad).now();
		
		int size = Iterators.size(descendants);
		
		Assert.assertEquals(size, 3);
	}
}
