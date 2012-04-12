package com.google.code.twig.annotation;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.Before;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.code.twig.LocalDatastoreTestCase;

public class AnnotationObjectDatastoreTest extends LocalDatastoreTestCase
{
	protected AnnotationObjectDatastore datastore;

	@Before
	public void datastore()
	{
		datastore = new AnnotationObjectDatastore();
	}

	@Test
	public void testLoadWithGaeKeyEncodedString()
	{
		GaeKeyStringModel instance = new GaeKeyStringModel();
		Key key = datastore.store(instance);
		assertNotNull(key);
		assertNotNull(instance.key);

		// Avoid any internal cache scenarios
		datastore = new AnnotationObjectDatastore();
		GaeKeyStringModel m = datastore.load(key);
		assertNotNull(m);
		assertEquals(KeyFactory.keyToString(key), m.getKey());
	}

	@Test
	public void testLoadWithGaeKey()
	{
		GaeKeyModel instance = new GaeKeyModel();
		Key key = datastore.store(instance);
		assertNotNull(key);
		assertNotNull(instance.key);

		// Avoid any internal cache scenarios
		datastore = new AnnotationObjectDatastore();
		GaeKeyModel m = datastore.load(key);
		assertNotNull(m);
		assertEquals(key, m.getKey());
	}

	/**
	 * Dummy test class to help with testing. Provides a model that has a @GaeKey
	 * encoded as a String.
	 */
	private static class GaeKeyStringModel
	{
		@GaeKey
		protected String key;

		public String getKey()
		{
			return key;
		}
	}

	private static class GaeKeyModel
	{
		@GaeKey
		protected Key key;

		public Key getKey()
		{
			return key;
		}
	}
}
