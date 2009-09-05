package com.vercer.engine.persist.conversion;

public interface TypeConverter<S, T>
{
	T convert(S source);
}
