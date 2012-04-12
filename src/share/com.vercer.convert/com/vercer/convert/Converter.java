package com.vercer.convert;

public interface Converter<S, T>
{
	public T convert(S source);
}
