package com.google.code.twig.util;

import com.google.code.twig.Restriction;
import com.google.common.base.Predicate;

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
