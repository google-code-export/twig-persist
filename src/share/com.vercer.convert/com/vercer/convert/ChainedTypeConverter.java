package com.vercer.convert;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ChainedTypeConverter extends BaseTypeConverter
{
	private List<TypeConverter> converters = new ArrayList<TypeConverter>();
	
	public ChainedTypeConverter()
	{
	}
	
	public void add(TypeConverter converter)
	{
		converters.add(converter);
	}
	
	@Override
	public <T> T convert(Object instance, Type source, Type target) throws CouldNotConvertException
	{
		// null is not valid because it is returned to signify not handled
		if (instance == null) return null;
		for (TypeConverter converter : converters)
		{
			@SuppressWarnings("unchecked")
			T converted = (T) converter.convert(instance, source, target);
			if (converted != null)
			{
				return converted;
			}
		}
		throw new IllegalStateException("Could not convert from " + source + " to " + target);
	}
}
