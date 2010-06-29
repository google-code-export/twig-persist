package com.vercer.engine.persist.translator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.Path.Part;
import com.vercer.engine.persist.conversion.TypeConverter;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.engine.persist.util.PropertySets.PrefixPropertySet;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.util.collections.MergeSet;

public class MapTranslator extends DecoratingTranslator
{
	private final TypeConverter converter;

	public MapTranslator(PropertyTranslator delegate, TypeConverter converter)
	{
		super(delegate);
		this.converter = converter;
	}
	
	@Override
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		// only try if we can set a map to the field
		if (!GenericTypeReflector.erase(type).isAssignableFrom(HashMap.class))
		{
			// pass on all other types down the chain
			return chained.propertiesToTypesafe(properties, path, type);
		}

		if (properties.isEmpty())
		{
			return NULL_VALUE;
		}
		
		// group the properties by prefix to create each item
		Collection<PrefixPropertySet> ppss = PropertySets.prefixPropertySets(properties, path);

		// find the types of the key and value from the generic parameters
		Type exact = GenericTypeReflector.getExactSuperType(type, Map.class);
		Type keyType = ((ParameterizedType) exact).getActualTypeArguments()[0];
		Type valueType = ((ParameterizedType) exact).getActualTypeArguments()[1];
		
		// type erasure means we can use object as the generic parameters
		Map<Object, Object> result = new HashMap<Object, Object>(ppss.size());
		for (PrefixPropertySet pps : ppss)
		{
			// the key must be converted from a String
			Part partAfterPrefix = pps.getPrefix().firstPartAfterPrefix(path);
			Object key = converter.convert(partAfterPrefix.getName(), keyType);
			
			// decode the value properties using the generic type info
			Object value = chained.propertiesToTypesafe(pps.getProperties(), pps.getPrefix(), valueType);
			
			result.put(key, value);
		}
		return result;
	}

	@Override
	public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
	{
		if (instance instanceof Map<?, ?> == false)
		{
			// pass it on down the line
			return chained.typesafeToProperties(instance, path, indexed);
		}
		
		Map<?, ?> map = (Map<?, ?>) instance;
		Set<?> keys = map.keySet();
		Set<Property> merged = new MergeSet<Property>(map.size());
		for (Object key: keys)
		{
			Object value = map.get(key);
			String keyString = converter.convert(key, String.class); 
			Path childPath = Path.builder(path).field(keyString).build();
			Set<Property> properties = chained.typesafeToProperties(value, childPath, indexed);
			
			if (properties == null)
			{
				// we could not handle a value so pass the whole map down the chain
				return chained.typesafeToProperties(instance, path, indexed);
			}
			
			merged.addAll(properties);
		}
		
		return merged;
	}

}
