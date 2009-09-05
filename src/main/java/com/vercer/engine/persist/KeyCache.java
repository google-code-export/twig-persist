package com.vercer.engine.persist;

import com.google.appengine.api.datastore.Key;

public interface KeyCache
{
	void clearKeyCache();

	Key evictEntity(Object reference);

	Object evictKey(Key key);
	
	void cache(Key key, Object instance);
}