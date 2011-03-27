package com.google.code.twig.util;

import com.google.code.twig.Path;
import com.google.code.twig.Property;

public class SimpleProperty implements Property
{
	protected Object value;
	private final Path path;
	private final boolean indexed;

	public SimpleProperty(Path path, Object value, boolean indexed)
	{
		this.path = path;
		this.value = value;
		this.indexed = indexed;
	}

	public Path getPath()
	{
		return this.path;
	}

	public Object getValue()
	{
		return this.value;
	}

	public boolean isIndexed()
	{
		return indexed;
	}

	@Override
	public String toString()
	{
		return Reflection.toString(this);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + (indexed ? 1231 : 1237);
		result = prime * result + (path == null ? 0 : path.hashCode());
		result = prime * result + (value == null ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (!(obj instanceof SimpleProperty))
		{
			return false;
		}
		SimpleProperty other = (SimpleProperty) obj;
		if (indexed != other.indexed)
		{
			return false;
		}
		if (path == null)
		{
			if (other.path != null)
			{
				return false;
			}
		}
		else if (!path.equals(other.path))
		{
			return false;
		}
		if (value == null)
		{
			if (other.value != null)
			{
				return false;
			}
		}
		else if (!value.equals(other.value))
		{
			return false;
		}
		return true;
	}

}