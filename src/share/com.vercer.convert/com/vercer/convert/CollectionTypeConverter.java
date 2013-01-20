package com.vercer.convert;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;

import com.google.code.twig.util.generic.Generics;

public class CollectionTypeConverter extends TypeConverter
{
	private final TypeConverter delegate;

	public CollectionTypeConverter(TypeConverter delegate)
	{
		this.delegate = delegate;
	}
	
	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		assert Generics.erase(source).isAssignableFrom(instance.getClass());
		
		Type sourceElementType;
		Type targetElementType;
		
		Class<?> sourceClass = Generics.erase(source);
		Class<?> targetClass = Generics.erase(target);
		
		if (Iterable.class.isAssignableFrom(sourceClass) && Collection.class.isAssignableFrom(targetClass))
		{
			sourceElementType = Generics.getTypeParameter(source, Iterable.class.getTypeParameters()[0]);
			if (sourceElementType == null)
			{
				sourceElementType = Object.class;
			}
			targetElementType = Generics.getTypeParameter(target, Collection.class.getTypeParameters()[0]);
			if (targetElementType == null)
			{
				targetElementType = Object.class;
			}
		}
		else
		{
			// we cannot convert this type
			return null;
		}
		
		Collection<Object> result = createCollectionInstance(targetClass, targetElementType);
		
		Iterable<?> iterable = (Iterable<?>) instance;
		for (Object object : iterable)
		{
			Object converted = delegate.convert(object, targetElementType);
			result.add(converted);
		}

		@SuppressWarnings("unchecked")
		T cast = (T) result;
		return cast;
	}

	private Collection<Object> createCollectionInstance(Class<?> targetClass, Type targetElementType)
	{
		if (targetClass.isAssignableFrom(ArrayList.class))
		{
			return new ArrayList<Object>();
		}
		if (targetClass.isAssignableFrom(LinkedList.class))
		{
			return new LinkedList<Object>();
		}
		if (targetClass.isAssignableFrom(HashSet.class))
		{
			return new HashSet<Object>();
		}
		else
		{
			throw new IllegalStateException();
		}
	}

	@Override
	public boolean converts(Type source, Type target)
	{
		return Iterable.class.isAssignableFrom(Generics.erase(source)) && 
				(Generics.erase(target).isAssignableFrom(HashSet.class) || 
				Generics.erase(target).isAssignableFrom(LinkedList.class) || 
				Generics.erase(target).isAssignableFrom(ArrayList.class));
	}
}
