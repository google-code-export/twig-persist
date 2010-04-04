package com.vercer.engine.persist.conversion;

import java.lang.reflect.Type;

public interface TypeConverter
{
	<T> T convert(Object source, Type type);
	Object nullValue = new Object();
}