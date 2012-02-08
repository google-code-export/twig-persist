package com.google.code.twig.util;

import java.util.Comparator;

import com.google.code.twig.Property;

public class PropertyPathComparator implements Comparator<Property>
{
	@Override
	public int compare(Property o1, Property o2)
	{
		return o1.getPath().compareTo(o2.getPath());
	}
}
