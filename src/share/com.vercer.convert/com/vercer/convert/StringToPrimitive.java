package com.vercer.convert;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author John Patterson (jdpatterson@gmail.com)
 */
public class StringToPrimitive implements Iterable<Converter<?, ?>>
{

	public Iterator<Converter<?, ?>> iterator()
	{
		List<Converter<?, ?>> converters = new ArrayList<Converter<?, ?>>();

		converters.add(new Converter<String, Integer>()
		{
			public Integer convert(String source)
			{
				return Integer.valueOf(source);
			}
		});
		converters.add(new Converter<String, Long>()
		{
			public Long convert(String source)
			{
				return Long.valueOf(source);
			}
		});
		converters.add(new Converter<String, Float>()
		{
			public Float convert(String source)
			{
				return Float.valueOf(source);
			}
		});
		converters.add(new Converter<String, Double>()
		{
			public Double convert(String source)
			{
				return Double.valueOf(source);
			}
		});
		converters.add(new Converter<String, Byte>()
		{
			public Byte convert(String source)
			{
				return Byte.valueOf(source);
			}
		});
		converters.add(new Converter<String, Boolean>()
		{
			public Boolean convert(String source)
			{
				// on is default value for html checkboxes
				return "on".equals(source) || Boolean.valueOf(source);
			}
		});
		converters.add(new Converter<String, Character>()
		{
			public Character convert(String source)
			{
				return source.charAt(0);
			}
		});

		return converters.iterator();
	}
}
