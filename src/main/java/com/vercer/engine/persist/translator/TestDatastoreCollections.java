package com.vercer.engine.persist.translator;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.vercer.engine.persist.LocalDatastoreTestCase;

public class TestDatastoreCollections extends LocalDatastoreTestCase
{
	@Test
	public void testCollections()
	{
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();

		Entity entity = new Entity("test");

		List<String> list = Arrays.asList("John", "is", "cool");
		Set<String> set = new HashSet<String>(list);

		entity.setProperty("list", set);

		service.put(entity);

		Query query = new Query("test").addFilter("list", FilterOperator.EQUAL, "cool");
		PreparedQuery prepared = service.prepare(query);
		Assert.assertEquals(1, prepared.countEntities());

		Entity result = prepared.asSingleEntity();

		Object value = result.getProperty("list");
		Assert.assertTrue(value instanceof List<?>);
	}
}
