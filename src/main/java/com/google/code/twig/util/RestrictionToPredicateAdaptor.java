package com.vercer.engine.persist.util;

import com.google.common.base.Predicate;
import com.vercer.engine.persist.Restriction;

public class RestrictionToPredicateAdaptor<T> implements Predicate<T>
{
	private final Restriction<T> restriction;
	
	public RestrictionToPredicateAdaptor(Restriction<T> restriction)
	{
		this.restriction = restriction;
	}
		
	public boolean apply(T input)
	{
		return restriction.allow(input);
	}
}
