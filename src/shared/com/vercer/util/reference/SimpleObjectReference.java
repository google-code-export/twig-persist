package com.vercer.util.reference;

import java.io.Serializable;

public class SimpleObjectReference<T> implements ObjectReference<T>, Serializable
{
	private T object;

	public SimpleObjectReference()
	{
	}
	
	public SimpleObjectReference(T object)
	{
		this.object = object;
	}
	
	public T get()
	{
		return object;
	}

	public void set(T object)
	{
		this.object = object;
	}
}
