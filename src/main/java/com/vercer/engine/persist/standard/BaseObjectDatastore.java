package com.vercer.engine.persist.standard;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.vercer.engine.persist.ObjectDatastore;

public abstract class BaseObjectDatastore implements ObjectDatastore
{
	private DatastoreService service;
	private Transaction transaction;

	public BaseObjectDatastore()
	{
		this.service = DatastoreServiceFactory.getDatastoreService();
	}
	
	public BaseObjectDatastore(DatastoreService service)
	{
		this.service = service;
	}

	public void setServiceConfig(DatastoreServiceConfig config)
	{
		this.service = DatastoreServiceFactory.getDatastoreService(config);
	}
	
	protected final Key servicePut(Entity entity)
	{
		if (transaction == null)
		{
			return service.put(entity);
		}
		else
		{
			return service.put(transaction, entity);
		}
	}
	
	protected final PreparedQuery servicePrepare(Query query)
	{
		if (transaction == null)
		{
			return service.prepare(query);
		}
		else
		{
			return service.prepare(transaction, query);
		}
	}
	
	protected final void serviceDelete(Collection<Key> keys)
	{
		if (transaction == null)
		{
			service.delete(keys);
		}
		else
		{
			service.delete(transaction, keys);
		}
	}

	protected final Entity serviceGet(Key key) throws EntityNotFoundException
	{
		if (transaction == null)
		{
			return service.get(key);
		}
		else
		{
			return service.get(transaction, key);
		}
	}
	
	protected final Map<Key, Entity> serviceGet(Iterable<Key> keys)
	{
		if (transaction == null)
		{
			return service.get(keys);
		}
		else
		{
			return service.get(transaction, keys);
		}
	}

	protected final List<Key> servicePut(Iterable<Entity> entities)
	{
		if (transaction == null)
		{
			return service.put(entities);
		}
		else
		{
			return service.put(transaction, entities);
		}
	}

	public final void setTransaction(Transaction transaction)
	{
		this.transaction = transaction;
	}

	public final Transaction getTransaction()
	{
		return transaction;
	}

	public final Transaction beginTransaction()
	{
		if (getTransaction() != null && getTransaction().isActive())
		{
			throw new IllegalStateException("Already in active transaction");
		}
		transaction = service.beginTransaction();
		return transaction;
	}

	public final void removeTransaction()
	{
		transaction = null;
	}
	
}
