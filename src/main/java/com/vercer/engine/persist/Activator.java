package com.vercer.engine.persist;

import java.util.Collection;

public interface Activator
{
	void activate(Object... instance);
	void activateAll(Collection<?> instances);
}
