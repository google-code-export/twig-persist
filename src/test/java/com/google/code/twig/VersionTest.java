package com.google.code.twig;

import java.util.Arrays;
import java.util.Map;

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
	public void storeSingleVersioned()
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
	
	@Test
	public void storeMultipleVersioned()
	{
		ObjectDatastore datastore = ObjectDatastoreFactory.createObjectDatastore();

		VersionedEntity entity1 = new VersionedEntity();
		VersionedEntity entity2 = new VersionedEntity();
		
		Assert.assertEquals(0, entity1.getVersion());

		Map<VersionedEntity, Key> stored = datastore.storeAll(Arrays.asList(entity1, entity2));
		
		// the version field should be updated
		Assert.assertEquals(1, entity1.getVersion());
		Assert.assertEquals(1, datastore.version(entity1));

		datastore.disassociateAll();

		// re-load the entity
		entity1 = datastore.load(stored.get(entity1));
		entity2 = datastore.load(stored.get(entity2));
		
		Assert.assertEquals(1, entity1.getVersion());
		Assert.assertEquals(1, datastore.version(entity1));
		
		datastore.updateAll(Arrays.asList(entity1, entity2));
		
		Assert.assertEquals(2, entity1.getVersion());
		Assert.assertEquals(2, datastore.version(entity1));
		Assert.assertEquals(2, entity2.getVersion());
		Assert.assertEquals(2, datastore.version(entity2));
	}
}
