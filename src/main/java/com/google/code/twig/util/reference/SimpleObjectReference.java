package com.google.code.twig.util.reference;

import java.io.Serializable;

public class SimpleObjectReference<T> implements ObjectReference<T>, Serializable
{
	private static final long serialVersionUID = 1L;
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
