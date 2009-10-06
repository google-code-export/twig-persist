package com.vercer.engine.persist;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

public interface TypesafeDatastore
{
	Key store(Object instance);

	Key store(Object instance, String name);

	Key store(Object instance, Key parentKey);

	Key store(Object instance, Key parentKey, String name);

	void update(Object instance, Key key);

	void delete(Key key);

	<T> Iterable<T> find(Query query);

	<T> Iterable<T> find(Class<T> type, Key parent);

	<T> T find(Class<T> type, Key parent, String name);

	<T> T find(Class<T> type, String name);

	Query query(Class<?> clazz);

	<T> T toTypesafe(Entity entity);

	<T> T load(Key key) throws EntityNotFoundException;

	DatastoreService getDatastore();

	Object encode(Object object);

}