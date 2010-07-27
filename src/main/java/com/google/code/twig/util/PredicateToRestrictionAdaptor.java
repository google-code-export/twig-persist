package com.google.code.twig.util;

import com.google.code.twig.Restriction;
import com.google.common.base.Predicate;

public class PredicateToRestrictionAdaptor<T> implements Restriction<T> 
{
	private final Predicate<T> predicate;

	public PredicateToRestrictionAdaptor(Predicate<T> predicate)
	{
		this.predicate = predicate;
	}
	
	@Override
	public boolean allow(T candidate)
	{
		return predicate.apply(candidate);
	}
}
