package com.vercer.engine.persist.conversion;

import com.google.appengine.api.datastore.Text;

public class StringToText implements TypeConverter<String, Text>
{
	public Text convert(String source)
	{
		return new Text(source);
	}
}
