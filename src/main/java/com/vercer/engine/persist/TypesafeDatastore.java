package com.vercer.engine.persist;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

public interface TypesafeDatastore
{
	Key store(Object instance);
	Key store(Object instance, String name);
	Key store(Object instance, Object parent);
	Key store(Object instance, Object parent, String name);

	<T> T find(Class<T> type, String name);
	<T> T find(Class<T> type, Object parent, String name);
	<T> Iterable<T> find(Query query);
	<T> Iterable<T> find(Query query, FetchOptions options);
	<T> Iterable<T> find(Class<T> type);
	<T> Iterable<T> find(Class<T> type, FetchOptions options);
	<T> Iterable<T> find(Class<T> type, Object parent);
	<T> Iterable<T> find(Class<T> type, Object parent, FetchOptions options);

	Query query(Class<?> clazz);
	DatastoreService getDatastore();
	Object encode(Object object);

	void update(Object instance);
	void delete(Object instance);

	void associate(Object instance);
	void disassociate(Object instance);
	Key associatedKey(Object instance);

	<T> T toTypesafe(Entity entity);
	<T> T load(Key key);
}
