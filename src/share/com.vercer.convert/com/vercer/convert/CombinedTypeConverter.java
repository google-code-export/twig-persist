package com.vercer.convert;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.primitives.Primitives;
import com.vercer.generics.Generics;
import com.vercer.util.Pair;

public class CombinedTypeConverter extends BaseTypeConverter implements ConverterRegistry
{
	private static final Converter<Object, Object> direct = new DirectConverter();

	private final Map<Pair<Type, Type>, Converter<?, ?>> sourceAndTargetToConverter
			= Maps.newConcurrentMap();

	@Override
	public void registerAll(Iterable<Converter<?, ?>> specifics)
	{
		for (Converter<?, ?> converter : specifics)
		{
			register(converter);
		}
	}

	@Override
	public void register(Converter<?, ?> converter)
	{
		Type source = sourceType(converter);
		Type target = targetType(converter);

		Pair<Type, Type> key = new Pair<Type, Type>(source, target);
		sourceAndTargetToConverter.put(key, converter);

		// start with the super class as we already added the class
		addGenericSuperTypes(converter, source, target);
	}

	private void addGenericSuperTypes(Converter<?, ?> converter, Type source, Type target)
	{
		Type[] superTypes = Generics.getExactDirectSuperTypes(target);
		for (Type superType : superTypes)
		{
			if (!superType.equals(Object.class))
			{
				sourceAndTargetToConverter.put(new Pair<Type, Type>(source, superType), converter);
				addGenericSuperTypes(converter, source, superType);
			}
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object input, Type source, Type target)
	{
		if (input == null) return null;
		
		if (target == Object.class)
		{
			return (T) input;
		}
		
		// check we already have the exact type
		if (source == target || target.equals(source))
		{
			return (T) input;
		}

		assert Primitives.wrap(Generics.erase(source)).isAssignableFrom(input.getClass());

		Converter<?, ?> converter = converter(source, target);

		if (converter == null)
		{
			return null;
		}

		return typesafe(converter, input);
	}
	
	private static final Converter<?, ?> NONE = new Converter<Object, Object>()
	{
		@Override
		public Object convert(Object instance)
		{
			throw new AssertionError();
		}
	};

	@Override
	public Converter<?, ?> converter(Type source, Type target)
	{
		source = ignoreWildCardParameters(source);
		target = ignoreWildCardParameters(target);

		// wrap all primitives
		if (target instanceof Class<?> && ((Class<?>) target).isPrimitive())
		{
			target = Primitives.wrap((Class<?>) target);
		}

		if (source instanceof Class<?> && ((Class<?>) source).isPrimitive())
		{
			source = Primitives.wrap((Class<?>) source);
		}

		Pair<Type, Type> pair = new Pair<Type, Type>(source, target);

		Converter<?, ?> converter = sourceAndTargetToConverter.get(pair);

		if (converter == NONE) return null;

		if (converter != null) return converter;

		// a target of object is a special case meaning converter decides what type to return
		if (target != Object.class && isSuperType(source, target))
		{
			converter = direct;
		}
		else
		{
			converter = breadth(source, target);
			if (converter == null)
			{
				// try special case target general converter
				converter = breadth(source, Object.class);
			}
		}

		// converter is null if no match registered
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

	private Converter<?, ?> breadth(Type source, Type target)
	{
		// returns only the direct super types
		Type[] directSuperTypes = Generics.getExactDirectSuperTypes(source);
		
		Type[] sourceTypes;

		// add object class if not already there
		if (directSuperTypes.length > 0 && directSuperTypes[0] != Object.class)
		{
			sourceTypes = new Type[directSuperTypes.length + 2];
			sourceTypes[sourceTypes.length - 1] = Object.class;
		}
		else
		{
			sourceTypes = new Type[directSuperTypes.length + 1];
		}
		sourceTypes[0] = source;
		
		System.arraycopy(directSuperTypes, 0, sourceTypes, 1, directSuperTypes.length);

		Converter<?, ?> result = converterForSourceSuperTypes(target, sourceTypes);

		if (result != null) return result;

		// now examine the children
		for (Type type : sourceTypes)
		{
			if (type != source && type != Object.class)
			{
				result = breadth(type, target);
				if (result != null)
				{
					return result;
				}
			}
		}

		// try with the raw type
		return null;
	}

	/**
	 * First try exact types, then erase the source, then the target, then both
	 */
	private Converter<?, ?> converterForSourceSuperTypes(Type target, Type[] superTypes)
	{
		// do a breadth first search for matching pair
		for (Type source : superTypes)
		{
			Pair<Type, Type> key = new Pair<Type, Type>(source, target);
			Converter<?, ?> result = sourceAndTargetToConverter.get(key);
			if (result != null)
			{
				return result;
			}

			if (source instanceof ParameterizedType)
			{
				key = new Pair<Type, Type>(Generics.erase(source), target);
				result = sourceAndTargetToConverter.get(key);
				if (result != null)
				{
					return result;
				}
			}

			if (target instanceof ParameterizedType)
			{
				key = new Pair<Type, Type>(source, Generics.erase(target));
				result = sourceAndTargetToConverter.get(key);
				if (result != null)
				{
					return result;
				}
			}

			if (target instanceof ParameterizedType && source instanceof ParameterizedType)
			{
				key = new Pair<Type, Type>(Generics.erase(source), Generics.erase(target));
				result = sourceAndTargetToConverter.get(key);
				if (result != null)
				{
					return result;
				}
			}

			if (target instanceof WildcardType && source instanceof ParameterizedType)
			{
				key = new Pair<Type, Type>(Generics.erase(source), target);
				result = sourceAndTargetToConverter.get(key);
				if (result != null)
				{
					return result;
				}
			}
		}
		return null;
	}

	private static boolean isSuperType(Type source, Type target)
	{
		// this is the first time we encounter this conversion
		Class<?> subClass = Generics.erase(source);
		Class<?> superClass = Generics.erase(target);

		// reflector assumes we have already checked classes are assignable
		if (!superClass.isAssignableFrom(subClass))
		{
			return false;
		}
		else if (source instanceof Class<?> && target instanceof Class<?>)
		{
			// both types are Classes (not generic types) and they are assignable
			return true;
		}

		// special cases for java util collections
		else if (List.class.isAssignableFrom(superClass) && source == Collections.EMPTY_LIST.getClass())
		{
			return true;
		}
		else if (Set.class.isAssignableFrom(superClass) && source == Collections.EMPTY_SET.getClass())
		{
			return true;
		}
		else if (superClass == Map.class && source == Collections.EMPTY_MAP.getClass())
		{
			return true;
		}

		else if (source instanceof Class<?> == false && target instanceof Class<?>)
		{
			// target is raw and source is not - would generate a warning
			return true;
		}
		else
		{
			// base classes are assignable so check type parameters
			return Generics.isSuperType(target, source);
		}
	}
	
	private static final TypeVariable<? extends Class<?>> sourceTypeParameter 
			= Converter.class.getTypeParameters()[0];
	private static final TypeVariable<? extends Class<?>> targetTypeParameter 
			= Converter.class.getTypeParameters()[1];

	public static Type targetType(Converter<?, ?> converter)
	{
		Type type = Generics.getTypeParameter(converter.getClass(), targetTypeParameter);
		return ignoreWildCardParameters(type);
	}

	public static Type sourceType(Converter<?, ?> converter)
	{
		Type type = Generics.getTypeParameter(converter.getClass(), sourceTypeParameter);
		return ignoreWildCardParameters(type);
	}

	public static Type ignoreWildCardParameters(Type type)
	{
		// treat Foo<?> the same as Foo.class
		if (type instanceof ParameterizedType)
		{
			// check if we have any non-wildcard parameters
			ParameterizedType parameterized = (ParameterizedType) type;
			Type[] arguments = parameterized.getActualTypeArguments();
			for (Type argument : arguments)
			{
				if (argument instanceof WildcardType == false)
				{
					// a parameter is not a wildcard so we cannot ignore them
					return type;
				}
			}
			
			// all parameters were wildcards so return the raw type
			return parameterized.getRawType();
		}
		return type;
	}
	
	@SuppressWarnings("unchecked")
	public static <S, T, R> R typesafe(Converter<S, T> converter, Object instance)
	{
		return (R) converter.convert((S) instance);
	}

}
