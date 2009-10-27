package com.vercer.engine.persist;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

/**
 * Useful subclass to quickly add support for equals, hasCode, and toString methods.
 * Because reflection is used these should not be used when performance is important but are
 * ideal for test data.
 *
 * @author John Patterson <john@vercer.com>
 */
public class ReflectiveObject
{
	private boolean trace;

	public ReflectiveObject()
	{
	}

	public ReflectiveObject(boolean trace)
	{
		this.trace = trace;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (EqualsBuilder.reflectionEquals(this, obj))
		{
			return true;
		}
		else
		{
			if (trace)
			{
				System.out.println("Not equal " + this + " != " + obj);
			}
			return false;
		}
	}

	@Override
	public int hashCode()
	{
		return HashCodeBuilder.reflectionHashCode(this);
	}

	@Override
	public String toString()
	{
		return ToStringBuilder.reflectionToString(this);
	}
}
