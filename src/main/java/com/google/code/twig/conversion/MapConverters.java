package com.google.code.twig.conversion;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.code.twig.annotation.Embedded;
import com.google.code.twig.util.Pair;
import com.google.code.twig.util.generic.Generics;
import com.vercer.convert.BaseTypeConverter;
import com.vercer.convert.Converter;
import com.vercer.convert.ConverterRegistry;

public class MapConverters
{
	@Embedded
	public final static class Entry<K, V>
	{
		K key;
		V value;
		
		protected Entry()
		{
		}
		
		public Entry(K key, V value)
		{
			this.key = key;
			this.value = value;
		}
	}
	
	public static class MapToList extends BaseTypeConverter
	{
		@SuppressWarnings("unchecked")
		@Override
		public <T> T convert(Object input, Type source, Type target)
		{
			Class<?> sourceClass = Generics.erase(source);
			Class<?> targetClass = Generics.erase(target);
			
			if (Map.class.isAssignableFrom(sourceClass) && targetClass.isAssignableFrom(ArrayList.class))
			{
				Map<?, ?> map = (Map<?, ?>) input;
				List<Entry<?, ?>> list = new ArrayList<Entry<?,?>>(map.size());
				Set<?> entrySet = map.entrySet();
				
				for (Object object : entrySet)
				{
					java.util.Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) object;
					list.add(new Entry<Object, Object>(entry.getKey(), entry.getValue()));
				}
				
				return (T) list;
			}
			else if (Collection.class.isAssignableFrom(sourceClass) && targetClass.isAssignableFrom(LinkedHashMap.class))
			{
				Collection<?> collection = (Collection<?>) input;
				LinkedHashMap<Object, Object> map = new LinkedHashMap<Object, Object>(collection.size());
				for (Object object : collection)
				{
					Entry<Object, Object> pair = (Entry<Object, Object>) object;
					map.put(pair.key, pair.value);
				}
				
				return (T) map;
			}
			return null;
		}
	}

	public static class EntrySetToMap implements Converter<List<?>, Map<?, ?>>
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
	
	public static void registerAll(ConverterRegistry converter)
	{
		converter.register(new EntrySetToMap());
	}
}
