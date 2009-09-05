package com.vercer.engine.persist.conversion;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.util.Pair;

public class TypeConverters
{
	private final List<TypeConverter<?, ?>> converters = new ArrayList<TypeConverter<?,?>>();
	private final Map<Pair<Type, Type>, TypeConverter<?, ?>> cache = new HashMap<Pair<Type,Type>, TypeConverter<?,?>>();

	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Type type)
	{
		TypeConverter<? super Object, ? extends T> converter =
			 (TypeConverter<? super Object, ? extends T>) converter(source.getClass(), type);

		if (converter == null)
		{
			throw new IllegalStateException("Cannot convert " + source + " to " + type);
		}
		return converter.convert(source);
	}

	public void register(TypeConverter<?, ?> converter)
	{
		converters.add(converter);
	}

	public TypeConverter<?, ?> converter(Type from, Type to)
	{
		Pair<Type, Type> pair = new Pair<Type, Type>(from, to);
		if (cache.containsKey(pair))
		{
			return cache.get(pair);
		}
		else
		{
			for (TypeConverter<?, ?> converter : converters)
			{
				Type type = GenericTypeReflector.getExactSuperType(converter.getClass(), TypeConverter.class);
				Type[] arguments = ((ParameterizedType) type).getActualTypeArguments();
				if (GenericTypeReflector.isSuperType(arguments[0], from) &&
						GenericTypeReflector.isSuperType(to, arguments[1]))
				{
					cache.put(pair, converter);
					return converter;
				}
			}
			return null;
		}
	}
}
