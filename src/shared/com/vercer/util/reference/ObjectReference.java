package com.vercer.util.reference;

public interface ObjectReference<T>
{
	T get();
	void set(T object);
}
