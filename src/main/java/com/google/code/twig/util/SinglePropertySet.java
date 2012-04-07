package com.google.code.twig.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.NoSuchElementException;

import com.google.code.twig.Path;
import com.google.code.twig.Property;

public class SinglePropertySet extends AbstractSet<Property> implements Property
{
	private final Path path;
	private final Object value;
	private final boolean indexed;

	public SinglePropertySet(Path path, Object value, boolean indexed)
	{
		this.path = path;
		this.value = value;
		this.indexed = indexed;
	}
	
	@Override
	public int compareTo(Property o)
	{
		return path.compareTo(o.getPath());
	}

	@Override
	public Iterator<Property> iterator()
	{
		return new Iterator<Property>()
		{
			boolean complete;

			public boolean hasNext()
			{
				return !complete;
			}

			public Property next()
			{
				if (hasNext())
				{
					complete = true;
					return new SimpleProperty(path, value, indexed);
				}
				else
				{
					throw new NoSuchElementException();
				}
			}

			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int size()
	{
		return 1;
	}

	public Object getValue()
	{
		return value;
	}

	public Path getPath()
	{
		return path;
	}

	public boolean isIndexed()
	{
		return indexed;
	}
}
