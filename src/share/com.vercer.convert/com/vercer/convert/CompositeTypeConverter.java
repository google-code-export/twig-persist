package com.vercer.convert;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.code.twig.util.generic.Generics;
import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import com.vercer.util.Pair;

public class CompositeTypeConverter extends TypeConverter
{
	private final Map<Pair<Type, Type>, TypeConverter> sourceAndTargetToConverter = createConverterCache();

	protected Map<Pair<Type, Type>, TypeConverter> createConverterCache()
	{
		return Maps.newHashMap();
	}
	
	private final List<TypeConverter> converters = new ArrayList<TypeConverter>();

	public void register(Converter<?, ?> converter)
	{
		converters.add(new ConverterAsTypeConverter(converter));
	}
	
	public void register(TypeConverter converter)
	{
		converters.add(converter);
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		if (instance == null)
		{
			return null;
		}

		// wrap all primitives
		if (target instanceof Class<?> && ((Class<?>) target).isPrimitive())
		{
			target = Primitives.wrap((Class<?>) target);
		}

		if (source instanceof Class<?> && ((Class<?>) source).isPrimitive())
		{
			source = Primitives.wrap((Class<?>) source);
		}
		
		assert Generics.erase(source).isAssignableFrom(instance.getClass());
		
		if (target == Object.class)
		{
			return (T) instance;
		}
		
		// check we already have the exact type
		if (source == target || target.equals(source))
		{
			return (T) instance;
		}

		TypeConverter converter = converter(source, target);

		if (converter == null)
		{
			throw new IllegalArgumentException("No converter from " + source + " to " + target);
		}
		
		return converter.convert(instance, source, target);
	}
	
	@Override
	public boolean converts(Type source, Type target)
	{
		return converter(source, target) != null;
	}
	
	private static final TypeConverter NONE = new TypeConverter()
	{
		@Override
		public boolean converts(Type source, Type target)
		{
			return false;
		}
		
		@Override
		public <T> T convert(Object instance, Type source, Type target)
		{
			throw new AssertionError();
		}
	};
	
	public TypeConverter converter(Type source, Type target)
	{
		Pair<Type, Type> pair = new Pair<Type, Type>(source, target);

		TypeConverter converter = sourceAndTargetToConverter.get(pair);

		if (converter == NONE) return null;

		if (converter != null) return converter;

		// find the first converter that can handle these types
		for (TypeConverter candidate : converters)
		{
			if (candidate.converts(source, target))
			{
				converter = candidate;
				break;
			}
		}
		
		if (converter == null)
		{
			// remember that none was found
			sourceAndTargetToConverter.put(pair, NONE);
		}
		else
		{
			sourceAndTargetToConverter.put(pair, converter);
		}

		return converter;
	}

}
