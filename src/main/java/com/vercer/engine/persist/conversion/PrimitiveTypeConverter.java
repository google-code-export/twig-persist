package com.vercer.engine.persist.conversion;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.vercer.engine.persist.util.generic.GenericTypeReflector;

public class PrimitiveTypeConverter implements TypeConverter
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
		Class<?> erased = GenericTypeReflector.erase(type);
		if (source == null && !erased.isPrimitive())
		{
			// consider null a primitive value
			return (T) nullValue;
		}
		
		// if we have a primitive or wrapper get the wrapper
		Class<?> wrapper = null;
		if (erased.isPrimitive())
		{
			wrapper = primitives.get(erased);
			if (source == null)
			{
				if (Number.class.isAssignableFrom(wrapper))
				{
					source = 0;
				}
				else if (Boolean.class == wrapper)
				{
					source = false;
				}
				else if (Character.class == wrapper)
				{
					 source = Character.MIN_VALUE;
				}
				else
				{
					throw new IllegalStateException("Unkonwn primitive default " + type);
				}
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
			// please tell me if you know a better way to do this!
			if (wrapper == Integer.class)
			{
				if (source instanceof Long)
				{
					return (T) Integer.valueOf(((Long) source).intValue());
				}
				else if (source instanceof Double)
				{
					return (T) Integer.valueOf(((Double) source).intValue());
				}
				else if (source instanceof Float)
				{
					return (T) Integer.valueOf(((Float) source).intValue());
				}
				else if (source instanceof Short)
				{
					return (T) Integer.valueOf(((Short) source).intValue());
				}
				else if (source instanceof Byte)
				{
					return (T) Integer.valueOf(((Byte) source).intValue());
				}
				else if (source instanceof String)
				{
					return (T) Integer.decode((String) source);
				}
			}
			else if (wrapper == Long.class)
			{
				if (source instanceof Integer)
				{
					return (T) Long.valueOf(((Integer) source).longValue());
				}
				else if (source instanceof Double)
				{
					return (T) Long.valueOf(((Double) source).longValue());
				}
				else if (source instanceof Float)
				{
					return (T) Long.valueOf(((Float) source).longValue());
				}
				else if (source instanceof Short)
				{
					return (T) Long.valueOf(((Short) source).longValue());
				}
				else if (source instanceof Byte)
				{
					return (T) Long.valueOf(((Byte) source).longValue());
				}
				else if (source instanceof String)
				{
					return (T) Long.decode((String) source);
				}
			}
			else if (wrapper == Float.class)
			{
				if (source instanceof Long)
				{
					return (T) Float.valueOf(((Long) source).floatValue());
				}
				else if (source instanceof Double)
				{
					return (T) Float.valueOf(((Double) source).floatValue());
				}
				else if (source instanceof Integer)
				{
					return (T) Float.valueOf(((Integer) source).floatValue());
				}
				else if (source instanceof Short)
				{
					return (T) Float.valueOf(((Short) source).floatValue());
				}
				else if (source instanceof Byte)
				{
					return (T) Float.valueOf(((Byte) source).floatValue());
				}
				else if (source instanceof String)
				{
					return (T) Float.valueOf((String) source);
				}
			}
			else if (wrapper == Double.class)
			{
				if (source instanceof Long)
				{
					return (T) Double.valueOf(((Long) source).doubleValue());
				}
				else if (source instanceof Integer)
				{
					return (T) Double.valueOf(((Integer) source).doubleValue());
				}
				else if (source instanceof Float)
				{
					return (T) Double.valueOf(((Float) source).doubleValue());
				}
				else if (source instanceof Short)
				{
					return (T) Double.valueOf(((Short) source).doubleValue());
				}
				else if (source instanceof Byte)
				{
					return (T) Double.valueOf(((Byte) source).doubleValue());
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
				if (source instanceof Long)
				{
					return (T) Byte.valueOf(((Long) source).byteValue());
				}
				else if (source instanceof Integer)
				{
					return (T) Byte.valueOf(((Integer) source).byteValue());
				}
				else if (source instanceof Float)
				{
					return (T) Byte.valueOf(((Float) source).byteValue());
				}
				else if (source instanceof Double)
				{
					return (T) Byte.valueOf(((Double) source).byteValue());
				}
				else if (source instanceof Short)
				{
					return (T) Byte.valueOf(((Short) source).byteValue());
				}
				else if (source instanceof String)
				{
					return (T) Byte.decode((String) source);
				}
			}

			throw new IllegalArgumentException("Could not convert from " + source + " to wrapper " + wrapper);
		}
	}

	public static Class<?> getWrapperClassForPrimitive(Class<?> primitive)
	{
		return primitives.get(primitive);
	}
}
