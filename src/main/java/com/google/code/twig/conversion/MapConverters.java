package com.google.code.twig.conversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.vercer.util.Pair;

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
}
