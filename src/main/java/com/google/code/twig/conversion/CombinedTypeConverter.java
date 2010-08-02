package com.google.code.twig.conversion;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.code.twig.util.generic.GenericTypeReflector;
import com.vercer.util.Pair;

public class CombinedTypeConverter implements TypeConverter
{	
	private static Map<Pair<Type, Class<?>>, Boolean> superTypes = new ConcurrentHashMap<Pair<Type, Class<?>>, Boolean>();

	private final List<SpecificTypeConverter<?, ?>> specifics = new ArrayList<SpecificTypeConverter<?,?>>();
	private final List<TypeConverter> generals = new ArrayList<TypeConverter>();
	private final Map<Pair<Type, Type>, SpecificTypeConverter<?, ?>> cache = new HashMap<Pair<Type,Type>, SpecificTypeConverter<?,?>>();

	/* (non-Javadoc)
	 * @see com.google.code.twig.conversion.TypeConverter#convert(java.lang.Object, java.lang.reflect.Type)
	 */
	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Type type)
	{
		// check if the instance can be returned directly
		if (source != null && isSuperType(type, source.getClass()))
		{
			// instance is a subtype so can be cast to required type
			return (T) source;
		}
		else
		{
			// try the specific converters first
			SpecificTypeConverter<? super Object, ? extends T> specific = null;
			if (source != null)
			{
				 specific = (SpecificTypeConverter<? super Object, ? extends T>) converter(source.getClass(), type);
			}
	
			if (specific == null)
			{
				// try the general converters
				for (TypeConverter general : generals)
				{
					Object result = general.convert(source, type);
					if (result != null)
					{
						if (result == nullValue)
						{
							return null;
						}
						else
						{
							return (T) result;
						}
					}
				}
	
				throw new IllegalStateException("Cannot convert " + source + " to " + type);
			}
			else
			{
				return specific.convert(source);
			}
		}
	}
	
	private static boolean isSuperType(Type type, Class<? extends Object> clazz)
	{
		if (type == clazz)
		{
			return true;
		}

		Pair<Type, Class<?>> key = new Pair<Type, Class<?>>(type, clazz);
		Boolean superType = superTypes.get(key);
		if (superType != null)
		{
			return superType;
		}
		else
		{
			boolean result = GenericTypeReflector.isSuperType(type, clazz);
			superTypes.put(key, result);
			return result;
		}
	}

	public void register(SpecificTypeConverter<?, ?> specific)
	{
		specifics.add(specific);
	}

	public void register(TypeConverter general)
	{
		generals.add(general);
	}

	public void prepend(TypeConverter converter)
	{
		generals.add(0, converter);
	}
	
	public void prepend(SpecificTypeConverter<?, ?> converter)
	{
		specifics.add(0, converter);
	}

	public SpecificTypeConverter<?, ?> converter(Type from, Type to)
	{
		Pair<Type, Type> pair = new Pair<Type, Type>(from, to);
		if (cache.containsKey(pair))
		{
			return cache.get(pair);
		}
		else
		{
			for (SpecificTypeConverter<?, ?> converter : specifics)
			{
				Type type = GenericTypeReflector.getExactSuperType(converter.getClass(), SpecificTypeConverter.class);
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
