package com.google.code.twig.conversion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import com.google.code.twig.util.Pair;
import com.google.code.twig.util.generic.GenericTypeReflector;

public class MapConverters
{
	public static class MapToEntrySet implements SpecificConverter<Map<?, ?>, List<?>>
	{
		@Override
		public List<?> convert(Map<?, ?> source)
		{
			List<Pair<?, ?>> pairs = new ArrayList<Pair<?,?>>(source.size());
			Set<?> entrySet = source.entrySet();
			for (Object object : entrySet)
			{
				Entry<?, ?> entry = (Entry<?, ?>) object;
				pairs.add(new Pair<Object, Object>(entry.getKey(), entry.getValue()));
			}
			return new ArrayList<Object>(pairs);
		}
	}

	public static class EntrySetToMap implements SpecificConverter<List<?>, Map<?, ?>>
	{
		@Override
		public Map<?, ?> convert(List<?> source)
		{
			HashMap<Object,Object> map = new HashMap<Object, Object>();
			@SuppressWarnings("unchecked")
			List<Pair<?, ?>> entries = (List<Pair<?, ?>>) source;
			for (Pair<?, ?> entry : entries)
			{
				map.put(entry.getFirst(), entry.getSecond());
			}
			return map;
		}
	}
	
	public static class MapKeyAndValueConverter implements TypeConverter
	{
		private final TypeConverter converter;
		public MapKeyAndValueConverter(TypeConverter converter)
		{
			this.converter = converter;
		}
		
		@SuppressWarnings("unchecked")
		@Override
		public <T> T convert(Object source, Type type)
		{
			Class<?> erased = GenericTypeReflector.erase(type);
			if (source instanceof Map<?, ?> && erased.isAssignableFrom(LinkedHashMap.class))
			{
				Type keyType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[0]);
				Type valueType = GenericTypeReflector.getTypeParameter(type, Map.class.getTypeParameters()[1]);
				Map<?, ?> map = (Map<?, ?>) source;
				
				Map<Object, Object> result = createMapInstance();
				for (Object key : map.keySet())
				{
					key = converter.convert(key, keyType);
					
					Object value = map.get(key);
					value = converter.convert(value, valueType);
					
					result.put(key, value);
				}
				
				return (T) result;
			}
			else
			{
				return null;
			}
		}

		protected Map<Object, Object> createMapInstance()
		{
			// preserve the ordering 
			return new LinkedHashMap<Object, Object>();
		}
	}

	public static void registerAll(CombinedConverter converter)
	{
		converter.append(new MapKeyAndValueConverter(converter));
		converter.append(new MapToEntrySet());
		converter.append(new EntrySetToMap());
	}
}
