package com.google.code.twig.conversion;

import java.lang.reflect.Type;

public interface TypeConverter
{
	<T> T convert(Object source, Type type);
	Object nullValue = new Object();
}