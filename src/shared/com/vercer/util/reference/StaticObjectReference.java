package com.vercer.util.reference;

public class StaticObjectReference<T> implements ObjectReference<T>
{
	private static Object object;

	@SuppressWarnings("unchecked")
	public T get()
	{
		return (T) object;
	}

	public void set(T object)
	{
		StaticObjectReference.object = object; 
	}
}
