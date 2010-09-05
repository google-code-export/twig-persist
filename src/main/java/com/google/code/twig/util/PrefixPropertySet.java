package com.google.code.twig.util;

import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;

public class PrefixPropertySet
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