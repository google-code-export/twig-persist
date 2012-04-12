package com.vercer.convert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

public class CollectionConverters implements Iterable<Converter<?, ?>>
{
	private ArrayList<Converter<?, ?>> converters;

	public CollectionConverters()
	{
		converters = new ArrayList<Converter<?, ?>>();
		converters.add(new Converter<Collection<?>, ArrayList<?>>()
		{
			@Override
			public ArrayList<?> convert(Collection<?> source)
			{
				return new ArrayList<Object>(source);
			}
		});
		converters.add(new Converter<Collection<?>, LinkedList<?>>()
		{
			@Override
			public LinkedList<?> convert(Collection<?> source)
			{
				return new LinkedList<Object>(source);
			}
		});
		converters.add(new Converter<Map<?, ?>, LinkedHashMap<?, ?>>()
		{
			@Override
			public LinkedHashMap<?, ?> convert(Map<?, ?> source)
			{
				return new LinkedHashMap<Object, Object>(source);
			}
		});
		converters.add(new Converter<Collection<?>, PriorityQueue<?>>()
		{
			@Override
			public PriorityQueue<?> convert(Collection<?> source)
			{
				return new PriorityQueue<Object>(source);
			}
		});
		converters.add(new Converter<Collection<?>, HashSet<?>>()
		{
			@Override
			public HashSet<?> convert(Collection<?> source)
			{
				return new HashSet<Object>(source);
			}
		});
	}

	@Override
	public Iterator<Converter<?, ?>> iterator()
	{
		return converters.iterator();
	}
}
