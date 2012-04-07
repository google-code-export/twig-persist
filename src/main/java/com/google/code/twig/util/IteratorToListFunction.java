package com.google.code.twig.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.common.base.Function;

public class IteratorToListFunction<T> implements Function<Iterator<T>, List<T>>, Serializable
{
	private static final long serialVersionUID = 1L;

	@Override
	public List<T> apply(Iterator<T> from)
	{
		List<T> result = new ArrayList<T>();
		while (from.hasNext())
		{
			result.add(from.next());
		}
		return result;
	}
}
