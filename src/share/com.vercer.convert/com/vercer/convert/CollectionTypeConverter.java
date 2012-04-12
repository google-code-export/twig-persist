package com.vercer.convert;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;

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
			targetElementType = Generics.getTypeParameter(target, Collection.class.getTypeParameters()[0]);
		}
		else
		{
			// we cannot convert this type
			return null;
		}
		
		Collection<Object> result = createCollectionInstance(targetClass);
		
		Iterable<?> iterable = (Iterable<?>) instance;
		for (Object object : iterable)
		{
			result.add(delegate.convert(object, sourceElementType, targetElementType));
		}

		@SuppressWarnings("unchecked")
		T cast = (T) result;
		return cast;
	}

	private Collection<Object> createCollectionInstance(Class<?> targetClass)
	{
		if (targetClass.isAssignableFrom(ArrayList.class))
		{
			return new ArrayList<Object>();
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
}
