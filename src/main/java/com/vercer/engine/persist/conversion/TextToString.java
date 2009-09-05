package com.vercer.engine.persist.conversion;

import com.google.appengine.api.datastore.Text;

public class TextToString implements TypeConverter<Text, String>
{
	public String convert(Text source)
	{
		return source.getValue();
	}
}
