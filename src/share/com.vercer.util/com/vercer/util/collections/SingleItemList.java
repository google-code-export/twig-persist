package com.vercer.util.collections;

import java.util.AbstractList;

public class SingleItemList<T> extends AbstractList<T>
{
	private T value;
	
	public SingleItemList(T value)
	{
		this.value = value;
	}
	
	@Override
	public T get(int index)
	{
		if (index != 0)
		{
			throw new IndexOutOfBoundsException();
		}
			
		return value;
	}

	@Override
	public int size()
	{
		return 1;
	}
}
