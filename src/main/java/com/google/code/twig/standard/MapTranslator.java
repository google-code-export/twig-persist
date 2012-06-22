package com.google.code.twig.standard;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Path.Part;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.translator.DecoratingTranslator;
import com.google.code.twig.util.PrefixPropertySet;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.collections.MergeSet;
import com.google.code.twig.util.generic.Generics;
import com.google.common.collect.Iterators;
import com.vercer.convert.TypeConverter;

public class MapTranslator extends DecoratingTranslator
{
	private final TypeConverter converter;
	private final TranslatorObjectDatastore datastore;

	public MapTranslator(TranslatorObjectDatastore datastore, PropertyTranslator delegate, TypeConverter converter)
	{
		super(delegate);
		this.datastore = datastore;
		this.converter = converter;
	}

	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		// only try if we can set a map to the field
		if (!Map.class.isAssignableFrom(Generics.erase(type)))
		{
			return null;
		}

		if (properties.size() == 1 && PropertySets.firstValue(properties) == null)
		{
			return NULL_VALUE;
		}

		if (properties.isEmpty())
		{
			return createMapInstance(0);
		}

		// group the properties by prefix to create each item
		Collection<PrefixPropertySet> ppss = PropertySets.prefixPropertySets(properties, path);

		// find the types of the key and value from the generic parameters
		Type exact = Generics.getExactSuperType(type, Map.class);
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

			// a value can exist if we are enhancing an existing map
			Object existingValue= result.get(key);
			if (existingValue != null)
			{
				// enhance the existing value rather than create a new one
				datastore.refresh = existingValue;
			}

			// decode the value properties using the generic type info
			Object decoded = delegate.decode(pps.getProperties(), pps.getPrefix(), valueType);

			// if we cannot convert every member of the list we fail
			if (decoded == null)
			{
				throw new IllegalStateException("Could not decode " + path + " to " + valueType);
			}

			if (decoded == NULL_VALUE)
			{
				decoded = null;
			}

			result.put(key, decoded);
		}
		return result;
	}

	protected Map<Object, Object> createMapInstance(int size)
	{
		if (datastore.refresh != null)
		{
			@SuppressWarnings("unchecked")
			Map<Object, Object> result = (Map<Object, Object>) datastore.refresh;
			datastore.refresh = null;
			return result;
		}
		else
		{
			return new HashMap<Object, Object>(size);
		}
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		if (instance == null) return Collections.emptySet();

		if (instance instanceof Map<?, ?> == false)
		{
			return null;
		}

		Map<?, ?> map = (Map<?, ?>) instance;
		Set<?> keys = map.keySet();
		Set<Property> merged = new MergeSet<Property>(map.size());
		for (Object key : keys)
		{
			Object value = map.get(key);
			String keyString = converter.convert(key, String.class);

			// the map key must be a valid part of a path
			if (Path.isValidName(keyString) == false)
			{
				throw new IllegalArgumentException("Invalid map key: " + keyString);
			}

			Path childPath = Path.builder(path).key(keyString).build();
			Set<Property> properties = delegate.encode(value, childPath, indexed);

			if (properties == null)
			{
				// we could not handle a value so pass down the chain
				return null;
			}

			merged.addAll(properties);
		}

		return merged;
	}

}
