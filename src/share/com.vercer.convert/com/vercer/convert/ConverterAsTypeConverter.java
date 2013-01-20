package com.vercer.convert;

import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

import com.vercer.generics.Generics;

public class ConverterAsTypeConverter extends TypeConverter
{
	private Converter<?, ?> converter;

	private static final TypeVariable<? extends Class<?>> sourceTypeParameter 
			= Converter.class.getTypeParameters()[0];
	private static final TypeVariable<? extends Class<?>> targetTypeParameter 
			= Converter.class.getTypeParameters()[1];

	private Type targetType;

	private Type sourceType;
	
	public ConverterAsTypeConverter(Converter<?, ?> converter)
	{
		this.converter = converter;
		targetType = targetType(converter);
		sourceType = sourceType(converter);
	}

	public static Type sourceType(Converter<?, ?> converter)
	{
		return Generics.getTypeParameter(converter.getClass(), sourceTypeParameter);
	}

	public static Type targetType(Converter<?, ?> converter)
	{
		return Generics.getTypeParameter(converter.getClass(), targetTypeParameter);
	}

	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		return typesafe(converter, instance);
	}
	
	@SuppressWarnings("unchecked")
	public static <S, T, R> R typesafe(Converter<S, T> converter, Object instance)
	{
		return (R) converter.convert((S) instance);
	}

	
	@Override
	public boolean converts(Type source, Type target)
	{	
		return isAssignableTo(source, sourceType) && isAssignableTo(targetType, target);
	}
}
