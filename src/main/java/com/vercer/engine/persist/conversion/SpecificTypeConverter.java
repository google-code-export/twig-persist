package com.vercer.engine.persist.conversion;

public interface SpecificTypeConverter<S, T>
{
	T convert(S source);
}
