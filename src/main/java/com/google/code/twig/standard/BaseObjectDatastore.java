package com.google.code.twig.standard;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.code.twig.ObjectDatastore;

public abstract class BaseObjectDatastore implements ObjectDatastore
{
	private DatastoreService service;
	private Transaction transaction;
	private final int retries;
	private static final Logger logger = Logger.getLogger(BaseObjectDatastore.class.getName());
	
	public BaseObjectDatastore(int retries)
	{
		this(DatastoreServiceConfig.Builder.withDefaults(), retries);
	}
	
	public BaseObjectDatastore(DatastoreServiceConfig config, int retries)
	{
		this.retries = retries;
		setConfiguration(config);
	}
	
	public void setConfiguration(DatastoreServiceConfig config)
	{
		this.service = newDatastoreService(config);
	}
	
	protected DatastoreService newDatastoreService(DatastoreServiceConfig config)
	{
		return DatastoreServiceFactory.getDatastoreService(config);
	}
	
	protected final Key servicePut(Entity entity)
	{
		if (transaction == null)
		{
			RuntimeException last = null;
			for (int tries = 0; tries < retries; tries++)
			{
				try
				{
					return service.put(entity);
				}
				catch (RuntimeException e)
				{
					last = e;
					logger.log(Level.WARNING, "Problem during try " + tries, e);
				}
			}
			
			throw last;
		}
		else
		{
			return service.put(transaction, entity);
		}
	}
	
	protected final List<Key> servicePut(Iterable<Entity> entities)
	{
		if (transaction == null)
		{
			RuntimeException last = null;
			for (int tries = 0; tries < retries; tries++)
			{
				try
				{
					return service.put(entities);
				}
				catch (RuntimeException e)
				{
					last = e;
					logger.log(Level.WARNING, "Problem during try " + tries, e);
				}
			}
			
			throw last;
		}
		else
		{
			return service.put(transaction, entities);
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
	
	public DatastoreService getService()
	{
		return service;
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
