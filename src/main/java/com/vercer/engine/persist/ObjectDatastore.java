package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;

public interface ObjectDatastore
{
	// fluent style methods
	StoreCommand store();
	FindCommand find();

	// convenience store methods
	Key store(Object instance);
	Key store(Object instance, String keyName);
	Key store(Object instance, String keyName, Object parent);
	Key store(Object instance, Object parent);
	<T> Map<T, Key> storeAll(Collection<? extends T> instances);
	<T> Map<T, Key> storeAll(Collection<? extends T> instances, Object parent);

	// convenience load methods
	<T> T load(Key key);
	<T> T load(Class<T> type, Object key);
	<T> T load(Class<T> type, Object key, Object parent);

	// convenience find methods
	<T> QueryResultIterator<T> find(Class<T> type);
	<T> QueryResultIterator<T> find(Class<T> type, Object ancestor);

	// convenience delete methods
	void delete(Object instance);
	void deleteAll(Type type);
	void deleteAll(Collection<?> instances);

	// activation
	int getActivationDepth();
	void setActivationDepth(int depth);
	void refresh(Object instance);
	
	void update(Object instance);
	void storeOrUpdate(Object instance);
	void storeOrUpdate(Object instance, Object parent);

	// cache control operations
	void associate(Object instance);
	void associate(Object instance, Key key);
	void disassociate(Object instance);
	void disassociateAll();
	Key associatedKey(Object instance);

	// type-safe to low-level bridge methods
	DatastoreService getService();
	Query query(Type type);
	<T> T toTypesafe(Entity entity);

	// transactions
	Transaction beginTransaction();
	Transaction getTransaction();
}
