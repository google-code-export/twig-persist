package com.vercer.convert;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Iterator;

import com.vercer.generics.Generics;

public class CollectionToArray extends TypeConverter
{
	private final TypeConverter delegate;

	public CollectionToArray(TypeConverter delegate)
	{
		this.delegate = delegate;
	}
	
	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		assert Generics.erase(source).isAssignableFrom(instance.getClass());
		
		Type targetElementType;
		
		Class<?> targetClass = Generics.erase(target);
		
		if (targetClass.isArray())
		{
			targetElementType = Generics.getArrayComponentType(target);
		}
		else
		{
			return null;
		}
		
		Collection<?> collection = (Collection<?>) instance;
		
		Object result = Array.newInstance(Generics.erase(targetElementType), collection.size());
		
		Iterator<?> iterator = collection.iterator();
		for (int i = 0; i < collection.size(); i++)
		{
			Object next = iterator.next();
			next = delegate.convert(next, targetElementType);
			Array.set(result, i, next);
		}
		
		@SuppressWarnings("unchecked")
		T cast = (T) result;
		return cast;
	}

	@Override
	public boolean converts(Type source, Type target)
	{
		return (target instanceof Class<?> && ((Class<?>) target).isArray()
				|| target instanceof GenericArrayType) && 
				Collection.class.isAssignableFrom(Generics.erase(source));
	}
}
