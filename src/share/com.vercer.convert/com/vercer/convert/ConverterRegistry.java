package com.vercer.convert;

import java.lang.reflect.Type;

public interface ConverterRegistry extends TypeConverter
{
	public abstract Converter<?, ?> converter(Type source, Type target);
	public abstract void register(Converter<?, ?> converter);
	public abstract void registerAll(Iterable<Converter<?, ?>> specifics);
}
