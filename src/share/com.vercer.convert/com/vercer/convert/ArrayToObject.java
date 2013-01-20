package com.vercer.convert;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;

public class ArrayToObject extends TypeConverter
{
	private TypeConverter converter;

	public ArrayToObject(TypeConverter converter)
	{
		this.converter = converter;
	}
	
	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		Object single = ((Object[]) instance)[0];
		return converter.convert(single, source, target);
	}

	@Override
	public boolean converts(Type source, Type target)
	{
		return source instanceof Class<?> && ((Class<?>) source).isArray() ||
				source instanceof GenericArrayType;
	}
}
