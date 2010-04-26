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

	public final DatastoreService getService()
	{
		return service;
	}

	protected Key servicePut(Entity entity)
	{
		return service.put(entity);
	}
	
	protected PreparedQuery servicePrepare(Query query)
	{
		return service.prepare(query);
	}
	
	protected void serviceDelete(Collection<Key> keys)
	{
		service.delete(keys);
	}

	protected Entity serviceGet(Key key) throws EntityNotFoundException
	{
		return service.get(key);
	}
	
	protected Map<Key, Entity> serviceGet(Iterable<Key> keys)
	{
		return service.get(keys);
	}

	protected List<Key> servicePut(Iterable<Entity> entities)
	{
		return service.put(entities);
	}
	
	
}
