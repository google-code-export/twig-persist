package com.vercer.util.collections;

import java.util.Set;


public abstract class ReadOnlyAdaptingSet<Target, Source> extends AdaptingSet<Target, Source>
{
	public ReadOnlyAdaptingSet(Set<Source> source)
	{
		super(source);
	}

	@Override
	protected final Source unwrap(Object target)
	{
		throw new UnsupportedOperationException();
	}
}
