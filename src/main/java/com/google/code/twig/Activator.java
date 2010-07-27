package com.google.code.twig;

import java.util.Collection;

public interface Activator
{
	void activate(Object... instances);
	void activateAll(Collection<?> instances);
}
