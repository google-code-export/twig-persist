package com.google.code.twig.conversion;

public interface SpecificConverter<S, T>
{
	T convert(S source);
}
