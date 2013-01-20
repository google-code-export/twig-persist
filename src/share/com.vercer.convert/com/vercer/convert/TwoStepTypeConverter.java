package com.vercer.convert;

import java.lang.reflect.Type;

public class TwoStepTypeConverter extends TypeConverter
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
	
	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		Object temp = converter.convert(instance, source, middle);
		return converter.convert(temp, middle, target);
	}

	@Override
	public boolean converts(Type source, Type target)
	{
		return this.source.equals(source) && this.target.equals(target);
	}
}
