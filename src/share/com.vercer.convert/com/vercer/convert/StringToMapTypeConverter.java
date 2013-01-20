package com.vercer.convert;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.vercer.generics.Generics;

public class StringToMapTypeConverter extends TypeConverter
{
	private final TypeConverter delegate;
	
	public StringToMapTypeConverter(TypeConverter delegate)
	{
		this.delegate = delegate;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object instance, Type source, Type target)
	{
		if (instance instanceof String && Map.class.isAssignableFrom(Generics.erase(target)))
		{
			Type keyType = Generics.getTypeParameter(target, Map.class.getTypeParameters()[0]);
			Type valueType = Generics.getTypeParameter(target, Map.class.getTypeParameters()[1]);
			
			Map<Object, Object> result = new HashMap<Object, Object>();
			
			String keysAndValues = ((String) instance).substring(1, ((String) instance).length() - 1);
			String[] keyValues = keysAndValues.split(", ");
			for (String keyValue : keyValues)
			{
				if (keyValue.isEmpty()) break;
				
				String[] keyAndValue = keyValue.split("=");
				
				Object key = delegate.convert(keyAndValue[0], keyType);
				Object value = delegate.convert(keyAndValue[1], valueType);
				
				result.put(key, value);
			}
			
			return (T) result;
		}
		else
		{
			return null;
		}
	}

	@Override
	public boolean converts(Type source, Type target)
	{
		return String.class == source && Map.class.isAssignableFrom(Generics.erase(target));
	}
}
