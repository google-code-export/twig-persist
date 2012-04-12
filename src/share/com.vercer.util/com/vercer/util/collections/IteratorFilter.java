package com.vercer.util.collections;

import java.util.Iterator;

import com.vercer.util.IteratorTranslatorFilter;

public abstract class IteratorFilter<T> extends IteratorTranslatorFilter<T, T>
{
	public IteratorFilter(Iterator<T> delegate)
	{
		super(delegate);
	}

	@Override
	protected T translate(T source)
	{
		return source;
	}
}
