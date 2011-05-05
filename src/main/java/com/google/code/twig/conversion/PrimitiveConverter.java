package com.google.code.twig.conversion;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.google.code.twig.util.generic.Generics;

public class PrimitiveConverter implements TypeConverter
{
	private static final Map<Class<?>, Class<?>> primitives = new HashMap<Class<?>, Class<?>>();
	private static final Set<Class<?>> wrappers = new HashSet<Class<?>>();

	static
	{
		primitives.put(Integer.TYPE, Integer.class);
		primitives.put(Long.TYPE, Long.class);
		primitives.put(Float.TYPE, Float.class);
		primitives.put(Double.TYPE, Double.class);
		primitives.put(Boolean.TYPE, Boolean.class);
		primitives.put(Short.TYPE, Short.class);
		primitives.put(Byte.TYPE, Byte.class);
		primitives.put(Character.TYPE, Character.class);

		wrappers.addAll(primitives.values());
	}


	@SuppressWarnings("unchecked")
	public <T> T convert(Object source, Type type)
	{
		Class<?> erased = Generics.erase(type);
		if (source == null && !erased.isPrimitive())
		{
			// consider null a primitive value
			return (T) NULL_VALUE;
		}
		
		// if we have a primitive or wrapper get the wrapper
		Class<?> wrapper = null;
		if (erased.isPrimitive())
		{
			wrapper = primitives.get(erased);
			if (source == null)
			{
				source = defaultPrimitiveValue(wrapper);
			}

			if (source.getClass() == wrapper)
			{
				// we do not need to convert a wrapper to primitive
				T result = (T) source;
				return result;
			}
		}
		else if (wrappers.contains(erased))
		{
			wrapper = erased;
		}

		// convert any primitives to string
		if (type == String.class && (primitives.containsKey(source.getClass()) || wrappers.contains(source.getClass())))
		{
			return (T) source.toString();
		}

		// we need a primitive wrapper so convert directly
		if (wrapper == null)
		{
			return null;  // signal that we cannot convert this type
		}
		else
		{
			if (Integer.class.isAssignableFrom(wrapper))
			{
				if (source instanceof Number)
				{
					return (T) Integer.valueOf(((Number) source).intValue());
				}
				else if (source instanceof String)
				{
					return (T) Integer.decode((String) source);
				}
			}
			else if (wrapper == Long.class)
			{
				if (source instanceof Number)
				{
					return (T) Long.valueOf(((Number) source).longValue());
				}
				else if (source instanceof String)
				{
					return (T) Long.decode((String) source);
				}
			}
			else if (wrapper == Float.class)
			{
				if (source instanceof Number)
				{
					return (T) Float.valueOf(((Number) source).floatValue());
				}
				else if (source instanceof String)
				{
					return (T) Float.valueOf((String) source);
				}
			}
			else if (wrapper == Double.class)
			{
				if (source instanceof Number)
				{
					return (T) Double.valueOf(((Number) source).doubleValue());
				}
				else if (source instanceof String)
				{
					return (T) Double.valueOf((String) source);
				}
			}
			else if (wrapper == Short.class)
			{
				if (source instanceof Number)
				{
					return (T) Short.valueOf(((Number) source).shortValue());
				}
				else if (source instanceof String)
				{
					return (T) Short.decode((String) source);
				}
			}
			else if (wrapper == Byte.class)
			{
				if (source instanceof Number)
				{
					return (T) Byte.valueOf(((Number) source).byteValue());
				}
				else if (source instanceof String)
				{
					return (T) Byte.decode((String) source);
				}
			}

			throw new IllegalArgumentException("Could not convert from " + source + " to wrapper " + wrapper);
		}
	}

	public static Object defaultPrimitiveValue(Class<?> wrapper)
	{
		if (Number.class.isAssignableFrom(wrapper))
		{
			return 0;
		}
		else if (Boolean.class == wrapper)
		{
			return false;
		}
		else if (Character.class == wrapper)
		{
			 return Character.MIN_VALUE;
		}
		else
		{
			throw new IllegalStateException("Unkonwn primitive default " + wrapper);
		}
	}

	public static Class<?> getWrapperClassForPrimitive(Class<?> primitive)
	{
		return primitives.get(primitive);
	}
}
