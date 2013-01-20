package com.vercer.convert;

import java.lang.reflect.Type;

import com.vercer.generics.Generics;

public abstract class TypeConverter
{
	@SuppressWarnings("unchecked")
	public <T> T convert(Object instance, Type target)
	{
		if (instance == null) return null;
		if (instance.getClass().equals(target)) return (T) instance; 
		return convert(instance, instance.getClass(), target);
	}
	
	public abstract <T> T convert(Object instance, Type source, Type target);
	
	public abstract boolean converts(Type source, Type target);

	protected static boolean isAssignableTo(Type source, Type target)
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
	
	public static final TypeConverter DIRECT = new TypeConverter()
	{
		@SuppressWarnings("unchecked")
		@Override
		public <T> T convert(Object instance, Type source, Type target)
		{
			return (T) instance;
		}

		@Override
		public boolean converts(Type source, Type target)
		{
			return isAssignableTo(source, target);
		}
	};
}
