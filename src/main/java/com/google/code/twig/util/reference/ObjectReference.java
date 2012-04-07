package com.google.code.twig.util.reference;

public interface ObjectReference<T>
{
	T get();
	void set(T object);
}
