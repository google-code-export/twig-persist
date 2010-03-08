package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;

public interface ActivationStrategy
{
	int activationDepth(Field field, int depth);
}
