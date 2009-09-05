package com.vercer.engine.persist.translator;

import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PrefixFilteringPropertySet;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.util.collections.MergeSet;

public class CollectionTranslator extends DecoratingTranslator
{
	public CollectionTranslator(PropertyTranslator chained)
	{
		super(chained);
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		Class<?> clazz = GenericTypeReflector.erase(type);
		Type componentType;
		if (clazz.isArray())
		{
			// handles generic arrays like
			componentType = GenericTypeReflector.getArrayComponentType(type);
		}
		else if (Collection.class.isAssignableFrom(clazz))
		{
			// handles the tricky task of finding what type of collection we have
			Type exact = GenericTypeReflector.getExactSuperType(type, Collection.class);
			componentType = ((ParameterizedType) exact).getActualTypeArguments()[0];
		}
		else
		{
			// signal that we cannot handle this type
			return null;
		}


		// test first property to see if we have an index number
		Property firstProperty = properties.iterator().next();
		if (firstProperty == null)
		{
			return null;
		}

		List<Object> objects;
		if (firstProperty.getValue() instanceof Collection<?>)
		{
			Collection<?> values = (Collection<?>) firstProperty.getValue();

			// need to decode the values
			objects = new ArrayList<Object>(values.size());

			for (Object	value : values)
			{
				Property property = new SimpleProperty(path, value, true);
				Set<Property> singleton = Collections.singleton(property);

				// just use the same path without an extra part for multi-value properties
				objects.add(chained.propertiesToTypesafe(singleton, path, componentType));
			}
		}
		else
		{
			// we have to read all properties with the same index
			int i = 0;
			objects = new ArrayList<Object>(properties.size() / 2);  // at least 2 properties per value
			while (true)
			{
				Path childPath = new Path.Builder(path).array(i).build();

				// filter properties for this index only
				PrefixFilteringPropertySet itemProperties = new PrefixFilteringPropertySet(childPath, properties);
				if (itemProperties.isEmpty())
				{
					// no properties with this index number so we are done
					break;
				}
				else
				{
					// add children with the indexed path
					objects.add(chained.propertiesToTypesafe(itemProperties, childPath, componentType));
				}
				i++;
			}
		}


		if (clazz.isArray())
		{
			Class<?> erased = GenericTypeReflector.erase(componentType);
			return objects.toArray((Object[]) Array.newInstance(erased, objects.size()));
		}
		else
		{
			// must be a collection
			if (Set.class.isAssignableFrom(clazz))
			{
				return new HashSet<Object>(objects);
			}
			else
			{
				return objects;
			}
		}
	}

	public Set<Property> typesafeToProperties(Object object, Path path, boolean indexed)
	{
		// try to convert object as an array
		Class<? extends Object> type = object.getClass();
		Object[] array = null;
		if (type.isArray())
		{
			if (type.getComponentType().isPrimitive())
			{
				throw new UnsupportedOperationException("Too lazy to implement primitve arrays yet");
			}
			array = (Object[]) object;
		}
		else if (Collection.class.isAssignableFrom(type))
		{
			array = ((Collection<?>) object).toArray();
		}

		if (array != null)
		{
			// we will create either a set of properties or a list of native values but not both
			Set<Property> merged = null;
			List<Object> values = null;
			for (int i = 0; i < array.length; i++)
			{
				Path indexPath = new Path.Builder(path).array(i).build();

				Set<Property> properties = chained.typesafeToProperties(array[i], indexPath, indexed);

				if (properties == null)
				{
					throw new IllegalStateException("Could not translate collection item: " + array[i]);
				}

				// was the instance encoded as a single native value
				if (properties.size() == 1)
				{
					// must be a single native value which we can put in a multi-value property
					if (values == null)
					{
						assert merged == null;  // assume that all properties also are single
						values = new ArrayList<Object>(array.length);
					}
					Property property = properties.iterator().next();
					values.add(property.getValue());
				}
				else
				{
					// more than one value so need to embed it by including an index to the path
					if (merged == null)
					{
						assert values == null;
						merged = new MergeSet<Property>(array.length);
					}
					merged.addAll(properties);
				}
			}

			if (values == null)
			{
				if (merged == null)
				{
					// the collection was empty
					return Collections.emptySet();
				}
				else
				{
					// return separate properties with index numbers in their paths
					return merged;
				}
			}
			else
			{
				// return the list of native values as a single multi-value property
				Property property = new SimpleProperty(path, values, indexed);
				return Collections.singleton(property);
			}
		}
		else
		{
			// we could not handle value as a collection so continue up the chain
			return chained.typesafeToProperties(object, path, indexed);
		}
	}

}
