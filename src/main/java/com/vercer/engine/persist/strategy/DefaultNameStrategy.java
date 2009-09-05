package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.vercer.engine.persist.util.generic.GenericTypeReflector;

public class DefaultNameStrategy implements NamingStrategy
{
	public Type kindToType(String kind)
	{
		try
		{
			return Class.forName(kind);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public String name(Field field)
	{
		String name = field.getName();
		if (name.charAt(0) == '_')
		{
			name = name.substring(1);
		}
		return name;
	}

	public String typeToKind(Type type)
	{
		Class<?> clazz = GenericTypeReflector.erase(type);
		return clazz.getName();
	}
	
	public boolean polyMorphic(Field field)
	{
		return true;
	}

}
