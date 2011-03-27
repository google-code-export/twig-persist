package com.google.code.twig.translator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Path.Part;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.conversion.TypeConverter;
import com.google.code.twig.util.PrefixPropertySet;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.collections.MergeSet;
import com.google.code.twig.util.generic.GenericTypeReflector;

public class MapTranslator extends DecoratingTranslator
{
	private final TypeConverter converter;

	public MapTranslator(PropertyTranslator delegate, TypeConverter converter)
	{
		super(delegate);
		this.converter = converter;
	}

	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		if (properties.isEmpty())
		{
			// do not decode empty missing properties
			return null;
		}
		
		if (PropertySets.firstValue(properties) == null)
		{
			return NULL_VALUE;
		}
		
		// only try if we can set a map to the field
		if (!GenericTypeReflector.erase(type).isAssignableFrom(HashMap.class))
		{
			// pass on all other types down the chain
			return chained.decode(properties, path, type);
		}

		// group the properties by prefix to create each item
		Collection<PrefixPropertySet> ppss = PropertySets.prefixPropertySets(properties, path);

		// find the types of the key and value from the generic parameters
		Type exact = GenericTypeReflector.getExactSuperType(type, Map.class);
		Type keyType = ((ParameterizedType) exact).getActualTypeArguments()[0];
		Type valueType = ((ParameterizedType) exact).getActualTypeArguments()[1];

		// type erasure means we can use object as the generic parameters
		int size = ppss.size();
		Map<Object, Object> result = createMapInstance(size);
		for (PrefixPropertySet pps : ppss)
		{
			// the key must be converted from a String
			Part partAfterPrefix = pps.getPrefix().firstPartAfterPrefix(path);
			Object key = converter.convert(partAfterPrefix.getName(), keyType);

			// decode the value properties using the generic type info
			Object value = chained.decode(pps.getProperties(), pps.getPrefix(), valueType);

			result.put(key, value);
		}
		return result;
	}

	protected Map<Object, Object> createMapInstance(int size)
	{
		Map<Object, Object> result = new HashMap<Object, Object>(size);
		return result;
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		if (instance instanceof Map<?, ?> == false)
		{
			// pass it on down the line
			return chained.encode(instance, path, indexed);
		}

		Map<?, ?> map = (Map<?, ?>) instance;
		Set<?> keys = map.keySet();
		Set<Property> merged = new MergeSet<Property>(map.size());
		for (Object key : keys)
		{
			Object value = map.get(key);
			String keyString = converter.convert(key, String.class);
			Path childPath = Path.builder(path).key(keyString).build();
			Set<Property> properties = chained.encode(value, childPath, indexed);

			if (properties == null)
			{
				// we could not handle a value so pass down the chain
				return chained.encode(instance, path, indexed);
			}

			merged.addAll(properties);
		}

		return merged;
	}

}
