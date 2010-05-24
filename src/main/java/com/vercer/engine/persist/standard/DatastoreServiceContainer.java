package com.vercer.engine.persist.standard;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

public class DatastoreServiceContainer
{
	private final DatastoreService service;

	public DatastoreServiceContainer(DatastoreService service)
	{
		this.service = service;
	}

	public DatastoreService getService()
	{
		return service;
	}

	protected Key servicePut(Entity entity)
	{
		return getService().put(entity);
	}
	
	protected PreparedQuery servicePrepare(Query query)
	{
		return getService().prepare(query);
	}
	
	protected void serviceDelete(Collection<Key> keys)
	{
		getService().delete(keys);
	}

	protected Entity serviceGet(Key key) throws EntityNotFoundException
	{
		return getService().get(key);
	}
	
	protected Map<Key, Entity> serviceGet(Iterable<Key> keys)
	{
		return getService().get(keys);
	}

	protected List<Key> servicePut(Iterable<Entity> entities)
	{
		return getService().put(entities);
	}
}
