package com.google.code.twig.conversion;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.ObjectDatastore;

public class KeyConverter implements SpecificConverter<Object, Key>
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
