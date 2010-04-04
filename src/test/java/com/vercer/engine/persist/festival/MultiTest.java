package com.vercer.engine.persist.festival;

import java.util.Arrays;

import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.repackaged.com.google.common.collect.Sets;
import com.vercer.engine.persist.LocalDatastoreTestCase;

public class MultiTest extends LocalDatastoreTestCase
{
	@Test
	public void testMultPropr() throws EntityNotFoundException
	{
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		Entity entity = new Entity("test");
		entity.setProperty("1", Arrays.asList("hi", "there", "john"));
		entity.setProperty("2", Sets.asIntegerSet(5, 9));
		Key put = service.put(entity);

		Entity entity2 = service.get(put);

		System.out.println(entity2.getProperty("2").getClass());
	}
}
