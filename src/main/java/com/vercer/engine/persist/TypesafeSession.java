package com.vercer.engine.persist;

import com.google.appengine.api.datastore.Key;

public interface TypesafeSession extends TypesafeDatastore
{
	Key store(Object instance, Object parent);
	Key store(Object instance, Object parent, String name);
	void update(Object instance);
	void delete(Object instance);
	<T> Iterable<T> find(Class<T> type, Object parent);
	<T> T find(Class<T> type, Object parent, String name);
	void associate(Object instance);
	void associate(Object instance, Key key);
	void disassociate(Object instance);
}
