package com.vercer.convert;

import java.lang.reflect.Type;

public class TwoStepTypeConverter extends BaseTypeConverter
{
	private final Type source;
	private final Type middle;
	private final Type target;
	private final TypeConverter converter;

	public TwoStepTypeConverter(TypeConverter converter, Type source, Type middle, Type target)
	{
		this.converter = converter;
		this.source = source;
		this.middle = middle;
		this.target = target;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object instance, Type source, Type target) throws CouldNotConvertException
	{
		if (source.equals(this.source) && target.equals(this.target))
		{
			Object temp = converter.convert(instance, source, middle);
			return (T) converter.convert(temp, middle, target);
		}
		
		return null;
	}
}
