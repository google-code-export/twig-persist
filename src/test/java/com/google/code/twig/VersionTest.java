package com.google.code.twig;

import junit.framework.Assert;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.annotation.Version;

public class VersionTest extends LocalDatastoreTestCase
{
	public VersionTest()
	{
		ObjectDatastoreFactory.register(VersionedEntity.class);
	}

	@Version
	public static class VersionedEntity
	{
		private long version;

		public long getVersion()
		{
			return this.version;
		}
	}

	@Test
	public void execute()
	{
		ObjectDatastore datastore = ObjectDatastoreFactory.createObjectDatastore();

		VersionedEntity entity = new VersionedEntity();
		
		Assert.assertEquals(0, entity.getVersion());

		Key stored = datastore.store(entity);
		
		Assert.assertEquals(1, entity.getVersion());
		Assert.assertEquals(1, datastore.version(entity));

		datastore.disassociateAll();

		entity = datastore.load(stored);
		
		Assert.assertEquals(1, entity.getVersion());
		Assert.assertEquals(1, datastore.version(entity));
		
		datastore.update(entity);
		
		Assert.assertEquals(2, entity.getVersion());
		Assert.assertEquals(2, datastore.version(entity));
	}
}
