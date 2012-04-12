package com.vercer.convert;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

public class SingletonListConverters implements Iterable<Converter<?, ?>>
{
	@Override
	public Iterator<Converter<?, ?>> iterator()
	{
		List<Converter<?, ?>> converters = new ArrayList<Converter<?, ?>>();

		converters.add(new Converter<Object, List<?>>()
		{
			@Override
			public List<?> convert(Object source)
			{
				return Arrays.asList(source);
			}
		});

		converters.add(new Converter<List<?>, Object>()
		{
			@Override
			public Object convert(List<?> target)
			{
				return target.get(0);
			}
		});

		return converters.iterator();
	}
}
