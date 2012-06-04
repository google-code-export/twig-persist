package com.google.code.twig;

import java.util.Collection;

public interface Activator
{
	void activate(Object instance);
	void activateAll(Collection<?> instances);
	boolean isActivated(Object instance);
	boolean isActivatable(Object instance);
}
