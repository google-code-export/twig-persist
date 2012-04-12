package com.vercer.convert;

import java.lang.reflect.Type;

public class DirectTypeConverter extends BaseTypeConverter
{
	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		return (T) instance;
	}
}
