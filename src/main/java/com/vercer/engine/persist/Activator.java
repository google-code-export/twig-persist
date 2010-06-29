package com.vercer.engine.persist;

import java.util.Collection;

public interface Activator
{
	void activate(Object... instances);
	void activateAll(Collection<?> instances);
}
