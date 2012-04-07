/**
 *
 */
package com.google.code.twig.util;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.common.base.Predicate;

public final class PathPrefixPredicate implements Predicate<Property>
{
	private final Path prefix;

	public PathPrefixPredicate(Path prefix)
	{
		this.prefix = prefix;
	}

	public boolean apply(Property property)
	{
		return property.getPath().hasPrefix(prefix);
	}
}