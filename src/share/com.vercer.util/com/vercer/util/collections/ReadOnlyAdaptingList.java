package com.vercer.util.collections;

import java.util.List;


public abstract class ReadOnlyAdaptingList<T, S> extends AdaptingList<T, S>
{
	public ReadOnlyAdaptingList(List<S> source)
	{
		super(source);
	}

	@Override
	protected S unwrap(T target)
	{
		throw new UnsupportedOperationException();
	}
}
