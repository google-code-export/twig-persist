package com.vercer.convert;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import com.google.code.twig.util.generic.Generics;

public class CollectionTypeConverter extends BaseTypeConverter
{
	private final TypeConverter delegate;

	public CollectionTypeConverter(TypeConverter delegate)
	{
		this.delegate = delegate;
	}
	
	@Override
	public <T> T convert(Object instance, Type source, Type target) throws CouldNotConvertException
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
		
		Collection<?> result = createCollectionInstance(targetClass, targetElementType);
		
		Iterable<?> iterable = (Iterable<?>) instance;
		for (Object object : iterable)
		{
			Object converted = delegate.convert(object, sourceElementType, targetElementType);
			typesafeAdd(result, converted);
		}

		@SuppressWarnings("unchecked")
		T cast = (T) result;
		return cast;
	}
	
	@SuppressWarnings("unchecked")
	private <E> void typesafeAdd(Collection<?> collection, Object element)
	{
		((Collection<E>) collection).add((E) element);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private Collection<?> createCollectionInstance(Class<?> targetClass, Type elementClass)
	{
		if (targetClass.isAssignableFrom(ArrayList.class))
		{
			return new ArrayList<Object>();
		}
		else if (targetClass.isAssignableFrom(HashSet.class))
		{
			return new HashSet<Object>();
		}
		else if (targetClass.isAssignableFrom(LinkedList.class))
		{
			return new LinkedList<Object>();
		}
		else if (targetClass.isAssignableFrom(PriorityQueue.class))
		{
			return new PriorityQueue<Object>();
		}
		else if (targetClass.isAssignableFrom(EnumSet.class))
		{
			return EnumSet.noneOf((Class<? extends Enum>) Generics.erase(targetClass));
		}
		else if (targetClass.isAssignableFrom(HashSet.class))
		{
			return new HashSet<Object>();
		}
		else
		{
			throw new IllegalStateException();
		}
	}
}
