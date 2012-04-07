package com.google.code.twig;

import java.util.Collection;

public interface Activator
{
	void activate(Object instance);
	void activateAll(Collection<?> instances);
//	void activate(Object instance, int depth);
//	void activateAll(Collection<?> instances, int depth);
	boolean isActivated(Object instance);
	boolean isActivatable(Object instance);
}
