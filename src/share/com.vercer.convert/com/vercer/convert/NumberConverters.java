package com.vercer.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author John Patterson (john@vercer.com)
 */
public class NumberConverters implements Iterable<Converter<?, ?>>
{
	public Iterator<Converter<?, ?>> iterator()
	{
		List<Converter<?, ?>> converters = new ArrayList<Converter<?, ?>>();

		converters.add(new Converter<Number, Integer>()
		{
			public Integer convert(Number source)
			{
				return Integer.valueOf(source.intValue());
			}
		});
		converters.add(new Converter<Number, Long>()
		{
			public Long convert(Number source)
			{
				return Long.valueOf(source.longValue());
			}
		});
		converters.add(new Converter<Number, Float>()
		{
			public Float convert(Number source)
			{
				return Float.valueOf(source.floatValue());
			}
		});
		converters.add(new Converter<Number, Double>()
		{
			public Double convert(Number source)
			{
				return Double.valueOf(source.doubleValue());
			}
		});
		converters.add(new Converter<Number, Short>()
		{
			public Short convert(Number source)
			{
				return Short.valueOf(source.shortValue());
			}
		});
		converters.add(new Converter<Number, BigInteger>()
		{
			public BigInteger convert(Number source)
			{
				return BigInteger.valueOf(source.longValue());
			}
		});
		converters.add(new Converter<Number, BigDecimal>()
		{
			public BigDecimal convert(Number source)
			{
				return BigDecimal.valueOf(source.doubleValue());
			}
		});

		return converters.iterator();
	}
}
