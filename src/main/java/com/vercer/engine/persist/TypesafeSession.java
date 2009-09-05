package com.vercer.engine.persist;

import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;

public interface TypesafeSession
{
	Key store(Object instance);

	Key store(Object instance, String name);

	Key store(Object instance, Key parentKey);

	Key store(Object instance, Key parentKey, String name);
	
	void update(Object instance);

	void update(Object instance, Key key);

	void delete(Key key);

	void delete(Object instance);
	
	<T> T find(String name, Class<T> type, Key parent);

	Object toTypesafe(Entity entity);

	<T> T find(String name, Class<T> type);

	Object load(Key key);

	DatastoreService getDatastore();

	<T> List<T> find(Query query);
	
	Object encode(Object object);

}