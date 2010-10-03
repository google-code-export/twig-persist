package com.google.code.twig.util;

import java.util.Comparator;

import com.google.code.twig.Property;

public class PropertyComparator implements Comparator<Property>
{
	@Override
	public int compare(Property o1, Property o2)
	{
		int pc = o1.getPath().compareTo(o2.getPath());
		if (pc != 0)
		{
			return pc;
		}
		else if (o1.getValue() instanceof Comparable<?>)
		{
			return compare((Comparable<?>) o1.getValue(), o2.getValue());
		}
		else
		{
			throw new IllegalArgumentException("Cannot compare " + o1 + " with " + o2);
		}
	}
	
	@SuppressWarnings("unchecked")
	private <T> int compare(Comparable<?> o1, T o2)
	{
		return ((Comparable<T>) o1).compareTo(o2);
	}
}
