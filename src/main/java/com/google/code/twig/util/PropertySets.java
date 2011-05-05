package com.google.code.twig.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Path.Part;
import com.google.code.twig.Property;
import com.google.code.twig.util.collections.ArraySortedSet;

public class PropertySets
{
	private static final PropertyComparator comparator = new PropertyComparator();
	
	@SuppressWarnings("unchecked")
	public static <T> T firstValue(Set<Property> properties)
	{
		return (T) firstProperty(properties).getValue();
	}
	
	public static <T> T uniqueValue(Set<Property> properties)
	{
		if (properties.size() != 1)
		{
			throw new IllegalStateException("Expected one property but was " + properties.size());
		}
		@SuppressWarnings("unchecked")
		T firstValue = (T) firstValue(properties);
		return firstValue;
	}

	public static Collection<PrefixPropertySet> prefixPropertySets(Set<Property> properties, Path prefix)
	{
		Property[] array = (Property[]) properties.toArray(new Property[properties.size()]);
		Collection<PrefixPropertySet> result = new ArrayList<PrefixPropertySet>();
		Part part = null;
		int start = 0;
		for (int i = 0; i < array.length; i++)
		{
			Path path = array[i].getPath();
			if (path.hasPrefix(prefix))
			{
				Part firstPartAfterPrefix = path.firstPartAfterPrefix(prefix);
				if (part != null && !firstPartAfterPrefix.equals(part))
				{
					// if the first part has changed then add a new set
					PrefixPropertySet ppf = createPrefixSubset(prefix, array, part, start, i);
					result.add(ppf);
					start = i; 
				}
				part = firstPartAfterPrefix;
			}
			else
			{
				part = null;
			}
		}
		
		// add the last set 
		if (array.length > 0 && part != null)
		{
			PrefixPropertySet ppf = createPrefixSubset(prefix, array, part, start, array.length);
			result.add(ppf);
		}
		return result;
	}

	private static PrefixPropertySet createPrefixSubset(Path prefix, Property[] array, Part part, int start, int i)
	{
		Set<Property> subset = new ArraySortedSet<Property>(array, start, i - start, comparator);
		PrefixPropertySet ppf = new PrefixPropertySet(Path.builder(prefix).append(part).build(), subset);
		return ppf;
	}

	public static Set<Property> create(Map<String, Object> properties, boolean indexed)
	{
		return new PropertyMapToSet(properties, indexed);
	}
	
	@SuppressWarnings("unchecked")
	public static <T> T valueForPath(String path, Set<Property> properties)
	{
		for (Property property : properties)
		{
			if (property.getPath().toString().equals(path))
			{
				return (T) property.getValue();
			}
		}
		return null;
	}

	public static Set<Property> singletonPropertySet(Path path, Object value, boolean indexed)
	{
		return new SinglePropertySet(path, value, indexed);
	}

	public static Property firstProperty(Set<Property> properties)
	{
		if (properties instanceof SinglePropertySet)
		{
			// optimised case for our own implementation
			return ((SinglePropertySet) properties);
		}
		else
		{
			Iterator<Property> iterator = properties.iterator();
			if (iterator.hasNext())
			{
				return iterator.next();
			}
			else
			{
				return null;
			}
		}		
	}
}
