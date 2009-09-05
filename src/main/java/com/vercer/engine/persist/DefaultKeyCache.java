package com.vercer.engine.persist;

import com.google.appengine.api.datastore.Key;
import com.vercer.util.reference.ObjectReference;

public class DefaultKeyCache implements KeyCache
{

	public void cache(Key key, Object instance)
	{
	}

	public void clearKeyCache()
	{
		// TODO Auto-generated method stub

	}

	public Key evictEntity(Object reference)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Object evictKey(Key key)
	{
		// TODO Auto-generated method stub
		return null;
	}

	public void cacheKeyReferenceForInstance(Object object, ObjectReference<Key> keyReference)
	{
		// TODO Auto-generated method stub
		
	}

}
