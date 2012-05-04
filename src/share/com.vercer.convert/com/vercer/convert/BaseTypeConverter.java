package com.vercer.convert;

import java.lang.reflect.Type;

public abstract class BaseTypeConverter implements TypeConverter
{
	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object instance, Type target) throws CouldNotConvertException
	{
		if (instance == null) return null;
		if (instance.getClass().equals(target)) return (T) instance; 
		return (T) convert(instance, instance.getClass(), target);
	}
}
