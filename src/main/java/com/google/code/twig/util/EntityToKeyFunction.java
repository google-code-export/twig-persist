package com.google.code.twig.util;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.common.base.Function;

public class EntityToKeyFunction implements Function<Entity, Key>
{
	@Override
	public Key apply(Entity arg0)
	{
		return arg0.getKey();
	}
}