package com.vercer.engine.toolkit.client;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;
import com.vercer.engine.persist.FindCommand;
import com.vercer.engine.persist.ObjectDatastore;
import com.vercer.engine.persist.StoreCommand;

public class ToolkitObjectDatastore extends ToolkitActivator implements ObjectDatastore
{
	@Override
	public void associate(Object instance)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void associate(Object instance, Key key)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public Key associatedKey(Object instance)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Transaction beginTransaction()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(Object instance)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll(Type type)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteAll(Collection<?> instances)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void disassociate(Object instance)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void disassociateAll()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public FindCommand find()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> QueryResultIterator<T> find(Class<T> type)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> QueryResultIterator<T> find(Class<T> type, Object ancestor)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getActivationDepth()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public DatastoreService getDefaultService()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Transaction getTransaction()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T load(Key key)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T load(Class<T> type, Object key)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> T load(Class<T> type, Object key, Object parent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Query query(Type type)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void refresh(Object instance)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void setActivationDepth(int depth)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public StoreCommand store()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Key store(Object instance)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Key store(Object instance, String keyName)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Key store(Object instance, String keyName, Object parent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Key store(Object instance, Object parent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Map<T, Key> storeAll(Collection<? extends T> instances)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <T> Map<T, Key> storeAll(Collection<? extends T> instances, Object parent)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void storeOrUpdate(Object instance)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void storeOrUpdate(Object instance, Object parent)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public <T> T toTypesafe(Entity entity)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update(Object instance)
	{
		// TODO Auto-generated method stub

	}

}
