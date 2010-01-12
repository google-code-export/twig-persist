package com.vercer.engine.persist.translator;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.google.appengine.repackaged.com.google.common.collect.AbstractIterator;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.engine.persist.util.SinglePropertySet;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;

public class ListTranslator extends DecoratingTranslator
{
	public ListTranslator(PropertyTranslator chained)
	{
		super(chained);
	}

	public Object propertiesToTypesafe(final Set<Property> properties, final Path path, Type type)
	{
		// only handle lists
		if (!List.class.isAssignableFrom(GenericTypeReflector.erase(type)))
		{
			// pass on all other types down the chain
			return chained.propertiesToTypesafe(properties, path, type);
		}

		if (properties.isEmpty())
		{
			return null;
		}

		// need to adapt a set of property lists into a list of property sets
		Iterator<Set<Property>> propertySets = new AbstractIterator<Set<Property>>()
		{
			int index;

			@Override
			protected Set<Property> computeNext()
			{
				boolean complete = true;
				Set<Property> result = new HashSet<Property>(properties.size());
				for (Property property : properties)
				{
					List<?> values = (List<?>) property.getValue();

					if (values.size() > index)
					{
						Object value = values.get(index);

						// null values are place holders for missing properties
						if (value != null)
						{
							result.add(new SimpleProperty(path, value, true));
						}

						// at least one property has items so we are not done yet
						complete = false;
					}
				}

				index++;

				if (complete)
				{
					return endOfData();
				}
				else
				{
					return result;
				}
			}
		};

		// handles the tricky task of finding what type of list we have
		Type exact = GenericTypeReflector.getExactSuperType(type, List.class);
		Type componentType = ((ParameterizedType) exact).getActualTypeArguments()[0];

		// decode each item of the list
		List<Object> objects = new ArrayList<Object>();
		while (propertySets.hasNext())
		{
			Object convertedChild = chained.propertiesToTypesafe(propertySets.next(), path, componentType);
			
			// if we cannot convert every member of the list we fail
			if (convertedChild == null)
			{
				return null;
			}
			
			objects.add(convertedChild);
		}

		// result will be converted to actual collection or array type
		return objects;
	}

	public Set<Property> typesafeToProperties(Object object, Path path, final boolean indexed)
	{
		if (object instanceof List<?>)
		{
			List<?> list = (List<?>) object;

			if (list.isEmpty())
			{
				return Collections.emptySet();
			}

			final Map<Path, List<Object>> lists = new HashMap<Path, List<Object>>(8);

			int count = 0;
			for (Object item : list)
			{
				if (item != null)
				{
					Set<Property> properties = chained.typesafeToProperties(item, path, indexed);

					if (properties == null)
					{
						// could not translate item or list
						return null;
					}

					for (Property property : properties)
					{
						List<Object> values = lists.get(property.getPath());
						if (values == null)
						{
							values = new ArrayList<Object>(4);

							lists.put(property.getPath(), values);
						}

						// need to pad the list with nulls if any values are missing
						while (values.size() < count)
						{
							values.add(null);
						}
						values.add(property.getValue());
					}
				}
				else
				{
					Collection<List<Object>> values = lists.values();
					for (List<Object> value : values)
					{
						value.add(null);
					}
				}
				count++;
			}

			// optimise for case of single properties
			if (lists.size() == 1)
			{
				Path childPath = lists.keySet().iterator().next();
				List<?> values = lists.get(childPath);
				return new SinglePropertySet(childPath, values, indexed);
			}
			else
			{
				return new AbstractSet<Property>()
				{
					@Override
					public Iterator<Property> iterator()
					{
						return new Iterator<Property>()
						{
							Iterator<Entry<Path, List<Object>>> iterator = lists.entrySet().iterator();

							public boolean hasNext()
							{
								return iterator.hasNext();
							}

							public Property next()
							{
								Entry<Path, List<Object>> next = iterator.next();
								return new SimpleProperty(next.getKey(), next.getValue(), indexed);
							}

							public void remove()
							{
								throw new UnsupportedOperationException();
							}
						};
					}

					@Override
					public int size()
					{
						return lists.size();
					}
				};
			}
		}
		else
		{
			// we could not handle value as a collection so continue up the chain
			return chained.typesafeToProperties(object, path, indexed);
		}
	}

}
