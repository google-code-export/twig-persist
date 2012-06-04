package com.vercer.convert;

import java.lang.reflect.Type;

public class AppendTypeConverter extends BaseTypeConverter
{
	private final TypeConverter delegate;
	private final Converter<?, ?> converter;
	private Type source;
	private Type target;

	public AppendTypeConverter(TypeConverter delegate, Converter<?, ?> converter)
	{
		this.delegate = delegate;
		this.converter = converter;
		this.source = CombinedTypeConverter.sourceType(converter);
		this.target = CombinedTypeConverter.targetType(converter);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object instance, Type source, Type target) throws CouldNotConvertException
	{
		if (this.source.equals(source) && this.target.equals(target))
		{
			return (T) CombinedTypeConverter.typesafe(converter, instance);
		}
		else
		{
			return delegate.convert(instance, source, target);
		}
	}
}
