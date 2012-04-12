package com.vercer.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class Reflection
{
	public static String toString(Object object)
	{
		StringBuilder builder = new StringBuilder();
		builder.append("[");

		String name = object.getClass().getName();
		builder.append(name.substring(name.lastIndexOf(".") + 1));
		builder.append(" ");
		Iterator<Field> fields = getAccessibleFields(object.getClass()).iterator();
		while (fields.hasNext())
		{
			Field field = fields.next();
			String fieldName = field.getName();
			if (fieldName.startsWith("_"))
			{
				fieldName = fieldName.substring(1);
			}
			builder.append(fieldName);
			builder.append("=");

			Object value;
			try
			{
				value = field.get(object);
			}
			catch (Exception e)
			{
				return "No permitted to access field";
			}

			if (value == null)
			{
				builder.append("null");
			}
			else
			{
				builder.append(value.toString());
			}
			if (fields.hasNext())
			{
				builder.append(" ");
			}
		}
		builder.append("]");

		return builder.toString();
	}
	
	public static List<Field> getAccessibleFields(Class<?> type)
	{
		List<Field> fields = new ArrayList<Field>();
		while (!Object.class.equals(type))
		{
			Field[] declaredFields = type.getDeclaredFields();
			for (Field field : declaredFields)
			{
				if (Modifier.isStatic(field.getModifiers()) == false)
				{
					// do not include system fields like $assertionsDisabled
					if (!field.getName().startsWith("$"))
					{
						fields.add(field);
						field.setAccessible(true);
					}
				}
			}
			type = type.getSuperclass();
		}
		return fields;
	}

	public static <T> T constructCopyWith(T original, Object... arguments) 
		throws	SecurityException,
						NoSuchMethodException, 
						InstantiationException,
						IllegalAccessException, 
						InvocationTargetException
	{
		@SuppressWarnings("unchecked")
		Constructor<T>[] constructors = (Constructor<T>[]) original.getClass().getConstructors();
		Constructor<T> constructor = null;
		next: for (Constructor<T> possible : constructors)
		{
			Class<?>[] parameterTypes = possible.getParameterTypes();
			if (parameterTypes.length == arguments.length)
			{
				for (int i = 0; i < parameterTypes.length; i++)
				{
					Class<?> type = parameterTypes[i];
					if (!type.isAssignableFrom(arguments[i].getClass()))
					{
						continue next;
					}
				}
				constructor = possible;
			}
		}
		
		if (constructor == null)
		{
			throw new IllegalArgumentException("Could not find constructor");
		}

		return constructor.newInstance(arguments);
	}
}
