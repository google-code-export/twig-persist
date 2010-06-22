package com.vercer.engine.persist.util;

import java.util.Iterator;
import java.util.Set;

import com.vercer.engine.persist.Property;

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
}
