package com.vercer.engine.persist.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;

public class PrefixFilteringPropertySet extends AbstractSet<Property>
{
	private final Set<Property> properties;
	private final Path prefix;

	public PrefixFilteringPropertySet(Path prefix, Set<Property> properties)
	{
		this.prefix = prefix;
		this.properties = properties;
	}

	@Override
	public Iterator<Property> iterator()
	{
		return Iterators.filter(properties.iterator(), new Predicate<Property>()
		{
			public boolean apply(Property source)
			{
				Path path = source.getPath();
				return path.equals(prefix) || path.hasPrefix(prefix);
			}
		});
	}

	@Override
	public int size()
	{
		return Iterators.size(iterator());
	}

	@Override
	public boolean isEmpty()
	{
		return !iterator().hasNext();
	}
}
