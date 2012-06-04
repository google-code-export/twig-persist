package com.vercer.convert;

import java.util.Arrays;
import java.util.List;

public class ArrayToList implements Converter<Object[], List<?>>
{
	@Override
	public List<?> convert(Object[] source)
	{
		return Arrays.asList(source);
	}
}
