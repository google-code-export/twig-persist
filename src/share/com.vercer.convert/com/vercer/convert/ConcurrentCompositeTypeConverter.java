package com.vercer.convert;

import java.lang.reflect.Type;
import java.util.Map;

import com.google.common.collect.Maps;
import com.vercer.util.Pair;

public class ConcurrentCompositeTypeConverter extends CompositeTypeConverter
{
	@Override
	protected Map<Pair<Type, Type>, TypeConverter> createConverterCache()
	{
		return Maps.newConcurrentMap();
	}
}
