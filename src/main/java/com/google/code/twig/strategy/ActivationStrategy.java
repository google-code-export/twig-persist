package com.google.code.twig.strategy;

import java.lang.reflect.Field;

public interface ActivationStrategy
{
	int activationDepth(Field field, int depth);
}
