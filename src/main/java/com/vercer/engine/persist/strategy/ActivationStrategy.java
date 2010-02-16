package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;

public interface ActivationStrategy
{
	boolean activate(Field field, int depth);
}
