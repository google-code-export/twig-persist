package com.vercer.engine.persist.strategy;

import java.lang.reflect.Type;

public interface VersionStrategy
{
	int version(Type type);
}
