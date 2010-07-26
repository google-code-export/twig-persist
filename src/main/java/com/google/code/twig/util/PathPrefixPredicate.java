/**
 *
 */
package com.vercer.engine.persist.util;

import com.google.common.base.Predicate;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;

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