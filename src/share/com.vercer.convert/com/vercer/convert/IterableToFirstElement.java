package com.vercer.convert;

import java.lang.reflect.Type;
import java.util.Iterator;

import com.google.code.twig.util.generic.Generics;

public class IterableToFirstElement extends BaseTypeConverter
{
	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		assert Generics.erase(source).isAssignableFrom(instance.getClass());
		
		if (instance instanceof Iterable)
		{
			Type sourceElementType = Generics.getTypeParameter(source, Iterable.class.getTypeParameters()[0]);
			Class<?> sourceElementClass = Generics.erase(sourceElementType);
			Class<?> targetClass = Generics.erase(target);
			
			if (targetClass.isAssignableFrom(sourceElementClass))
			{
				Iterator<?> iterator = ((Iterable<?>) instance).iterator();
				if (iterator.hasNext())
				{
					return (T) iterator.next();
				}
			}
		}
		
		return null;
	}
}
