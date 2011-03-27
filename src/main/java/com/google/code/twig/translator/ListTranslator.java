package com.google.code.twig.translator;

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
import java.util.Map.Entry;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.SimpleProperty;
import com.google.code.twig.util.SinglePropertySet;
import com.google.code.twig.util.generic.Generics;
import com.google.common.collect.Lists;

public class ListTranslator extends DecoratingTranslator
{
	public ListTranslator(PropertyTranslator chained)
	{
		super(chained);
	}

	public Object decode(final Set<Property> properties, final Path path, Type type)
	{
		// only handle lists
		if (!Generics.erase(type).isAssignableFrom(ArrayList.class))
		{
			// pass on all other types down the chain
			return chained.decode(properties, path, type);
		}

		if (properties.isEmpty())
		{
			// do not decode empty missing properties
			return null;
		}
		
		if (PropertySets.firstValue(properties) == null)
		{
			return NULL_VALUE;
		}
		
		// need to adapt a set of property lists into a list of property sets
		List<Set<Property>> propertySets = Lists.newArrayList();
		boolean complete = false;
		for (int index = 0; !complete; index++)
		{
			complete = true;
			Set<Property> result = new HashSet<Property>(properties.size());
			for (Property property : properties)
			{
				List<?> values;
				Path itemPath = property.getPath();
				Object list = property.getValue();
				
				// every property should be of the same type but just repeat check
				if (list instanceof List<?>)
				{
					// we have a list of property values
					values = (List<?>) list;
				}
				else
				{
					// we could not handle this value so pass the whole thing down the line
					return chained.decode(properties, path, type);
				}

				if (values.size() > index)
				{
					Object value = values.get(index);

					// null values are place holders for missing properties
					if (value != null)
					{
						result.add(new SimpleProperty(itemPath, value, true));
					}
					// at least one property has items so we are not done yet
					complete = false;
				}
			}

			if (complete == false)
			{
				propertySets.add(result);
			}
		}

		// handles the tricky task of finding what type of list we have
		Type exact = Generics.getExactSuperType(type, List.class);
		Type componentType = ((ParameterizedType) exact).getActualTypeArguments()[0];

		// decode each item of the list
		List<Object> objects = new ArrayList<Object>();
		for (Set<Property> itemProperties : propertySets)
		{
			Object convertedChild = chained.decode(itemProperties, path, componentType);

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

	public Set<Property> encode(Object object, Path path, boolean indexed)
	{
		if (object == null)
		{
			return PropertySets.singletonPropertySet(path, null, indexed);
		}
		
		if (object instanceof List<?>)
		{
			List<?> list = (List<?>) object;

			if (list.isEmpty())
			{
				return Collections.emptySet();
			}

			final Map<Path, List<Property>> lists = new HashMap<Path, List<Property>>(8);

			int count = 0;
			for (Object item : list)
			{
				if (item != null)
				{
					Set<Property> properties = chained.encode(item, path, indexed);

					if (properties == null)
					{
						// we could not handle so continue up the chain
						return chained.encode(object, path, indexed);
					}

					for (Property property : properties)
					{
						Path itemPath = property.getPath();

						List<Property> values = lists.get(itemPath);
						if (values == null)
						{
							values = new ArrayList<Property>(4);

							lists.put(itemPath, values);
						}

						// need to pad the list with nulls if any values are missing
						while (values.size() < count)
						{
							values.add(null);
						}
						values.add(property);
					}
				}
				else
				{
					Collection<List<Property>> values = lists.values();
					for (List<Property> value : values)
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
				List<Property> properties = lists.get(childPath);
				List<Object> values = new ArrayList<Object>(properties.size());
				for (Property property : properties)
				{
					values.add(property.getValue());
					
					// should be the same for all properties
					indexed = property.isIndexed();
				}
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
							Iterator<Entry<Path, List<Property>>> iterator = lists.entrySet().iterator();

							public boolean hasNext()
							{
								return iterator.hasNext();
							}

							public Property next()
							{
								Entry<Path, List<Property>> next = iterator.next();
								
								// extract the values
								List<Property> properties = next.getValue();
								boolean indexed = false;
								List<Object> values = new ArrayList<Object>(properties.size());
								for (Property property : properties)
								{
									Object value = null;
									if (property != null)
									{
										value = property.getValue();
										// should be the same for all properties
										indexed = property.isIndexed();
									}
									values.add(value);
								}
								return new SimpleProperty(next.getKey(), values, indexed);
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
			return chained.encode(object, path, indexed);
		}
	}

}
