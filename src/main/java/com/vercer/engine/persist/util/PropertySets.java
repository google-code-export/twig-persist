package com.vercer.engine.persist.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.Path.Part;
import com.vercer.util.collections.ArraySortedSet;

public class PropertySets
{
	@SuppressWarnings("unchecked")
	public static <T> T firstValue(Set<Property> properties)
	{
		if (properties instanceof SinglePropertySet)
		{
			// optimised case for our own implementation
			return (T) ((SinglePropertySet) properties).getValue();
		}
		else
		{
			Iterator<Property> iterator = properties.iterator();
			Property property = iterator.next();
			if (property == null)
			{
				return null;
			}
			else
			{
				return (T) property.getValue();
			}
		}
	}

	public static class PrefixPropertySet
	{
		private Path prefix;
		private Set<Property> properties;
		public PrefixPropertySet(Path prefix, Set<Property> properties)
		{
			super();
			this.prefix = prefix;
			this.properties = properties;
		}

		public Path getPrefix()
		{
			return prefix;
		}
		
		public Set<Property> getProperties()
		{
			return properties;
		}
	}
	
	public static Collection<PrefixPropertySet> prefixPropertySets(Set<Property> properties, Path prefix)
	{
		Collection<PrefixPropertySet> result = new ArrayList<PrefixPropertySet>();
		Property[] array = (Property[]) properties.toArray(new Property[properties.size()]);
		Part part = null;
		int start = 0;
		for (int i = 0; i < array.length; i++)
		{
			Part firstPartAfterPrefix = array[i].getPath().firstPartAfterPrefix(prefix);
			if (part != null && !firstPartAfterPrefix.equals(part))
			{
				// if the first part has changed then add a new set
				PrefixPropertySet ppf = createPrefixSubset(prefix, array, part, start, i);
				result.add(ppf);
				start = i; 
			}
			part = firstPartAfterPrefix;
		}
		
		// add the last set 
		if (array.length > 0)
		{
			PrefixPropertySet ppf = createPrefixSubset(prefix, array, part, start, array.length);
			result.add(ppf);
		}
		return result;
	}

	private static PrefixPropertySet createPrefixSubset(Path prefix, Property[] array, Part part,
			int start, int i)
	{
		Set<Property> subset = new ArraySortedSet<Property>(array, start, i - start);
		PrefixPropertySet ppf = new PrefixPropertySet(Path.builder(prefix).append(part).build(), subset);
		return ppf;
	}

	public static Set<Property> create(Map<String, Object> properties, boolean indexed)
	{
		return new PropertyMapToSet(properties, indexed);
	}
}
