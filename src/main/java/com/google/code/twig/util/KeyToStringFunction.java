package com.google.code.twig.util;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.base.Function;

public class KeyToStringFunction implements Function<Key, String>
{
	@Override
	public String apply(Key from)
	{
		return KeyFactory.keyToString(from);
	}
}
