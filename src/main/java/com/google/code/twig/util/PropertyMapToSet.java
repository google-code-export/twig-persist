/**
 *
 */
package com.google.code.twig.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import com.google.code.twig.Path;
import com.google.code.twig.Property;

public final class PropertyMapToSet extends AbstractSet<Property>
{
	private final Map<String, Object> properties;
	private final boolean indexed;

	public PropertyMapToSet(Map<String, Object> properties, boolean indexed)
	{
		this.properties = properties;
		this.indexed = indexed;
	}

	@Override
	public Iterator<Property> iterator()
	{
		final Iterator<Entry<String, Object>> iterator = properties.entrySet().iterator();
		return new Iterator<Property>()
		{
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			public Property next()
			{
				Entry<String, Object> next = iterator.next();
				return new SimpleProperty(new Path(next.getKey()), next.getValue(), indexed);
			}

			public void remove()
			{
				iterator.remove();
			}
		};
	}

	@Override
	public int size()
	{
		return properties.size();
	}
}