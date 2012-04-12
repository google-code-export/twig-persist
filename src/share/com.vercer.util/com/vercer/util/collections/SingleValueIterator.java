package com.vercer.util.collections;

import java.util.Iterator;

public class SingleValueIterator<T> implements Iterator<T>
{
	private T value;

	public SingleValueIterator(T value)
	{
		this.value = value;
	}
	
	public boolean hasNext()
	{
		return value != null;
	}

	public T next()
	{
		T result = value;
		value = null;
		return result;
	}

	public void remove()
	{
		value = null;
	}

}
