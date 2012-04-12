package com.vercer.convert;


public class ObjectToString implements Converter<Object, String>
{
	@Override
	public String convert(Object source)
	{
		return source.toString();
	}
}
