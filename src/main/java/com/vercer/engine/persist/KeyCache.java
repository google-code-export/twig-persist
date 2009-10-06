package com.vercer.engine.persist;

import com.google.appengine.api.datastore.Key;

public interface KeyCache
{
	void clearKeyCache();

	Key evictEntity(Object entity);

	Object evictKey(Key key);

	void cache(Key key, Object entity);

	<T> T getCachedEntity(Key key);

	Key getCachedKey(Object entity);
}