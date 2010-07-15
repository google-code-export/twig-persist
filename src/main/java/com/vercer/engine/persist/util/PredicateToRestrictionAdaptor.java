package com.vercer.engine.persist.util;

import com.google.common.base.Predicate;
import com.vercer.engine.persist.Restriction;

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
