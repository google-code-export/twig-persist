package com.vercer.util.reference;

public abstract class ReadOnlyObjectReference<T> implements ObjectReference<T>
{
	public void set(T object)
	{
		throw new UnsupportedOperationException();
	};
}
