package com.vercer.util.reference;

public class ThreadLocalObjectReference<T> implements ObjectReference<T>
{
	private ThreadLocal<T> localObjects = new ThreadLocal<T>();
	
	public T get()
	{
		return localObjects.get();
	}

	public void set(T object)
	{
		localObjects.set(object);
	}
}
