package com.google.code.twig.conversion;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.ObjectDatastore;
import com.vercer.convert.Converter;

public class KeyConverter implements Converter<Object, Key>
{
	private final ObjectDatastore datastore;

	public KeyConverter(ObjectDatastore datastore)
	{
		this.datastore = datastore;
	}

	public Key convert(Object source)
	{
		return datastore.associatedKey(source);
	}
}
