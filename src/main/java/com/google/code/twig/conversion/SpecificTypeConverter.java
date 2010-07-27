package com.google.code.twig.conversion;

public interface SpecificTypeConverter<S, T>
{
	T convert(S source);
}
