package com.google.code.twig.util;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.common.base.Function;

public class StringToKeyFunction implements Function<String, Key>
{
	@Override
	public Key apply(String from)
	{
		return KeyFactory.stringToKey(from);
	}
}
