package com.vercer.convert;


public class DirectConverter implements Converter<Object, Object>
{
	@Override
	public Object convert(Object instance)
	{
		return instance;
	}
}
