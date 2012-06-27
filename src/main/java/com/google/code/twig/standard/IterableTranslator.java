package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.translator.DecoratingTranslator;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.SimpleProperty;
import com.google.code.twig.util.SinglePropertySet;
import com.google.code.twig.util.generic.Generics;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class IterableTranslator extends DecoratingTranslator
{
	private final TranslatorObjectDatastore datastore;

	public IterableTranslator(TranslatorObjectDatastore datastore, PropertyTranslator chained)
	{
		super(chained);
		this.datastore = datastore;
	}

	public Object decode(final Set<Property> properties, final Path path, Type type)
	{
		if (!Iterable.class.isAssignableFrom(Generics.erase(type)))
		{
			return null;
		}

		if (properties.size() == 1)
		{
			if (PropertySets.firstValue(properties) == null)
			{
				return NULL_VALUE;
			}
			else
			{
				// collections always come back as ArrayLists
				Collection<Object> collection = PropertySets.firstValue(properties);
				if (collection != null && Iterables.getFirst(collection, null) instanceof Key)
				{
					// leave keys for the relation translator which can load all at once
					return null;
				}
			}	
		}
		
		Collection<Object> list = createCollection(type);

		if (properties.isEmpty())
		{
			return list;
		}

		List<Set<Property>> propertySets = decodePropertySets(properties);

		// handles the tricky task of finding what type of list we have
		Type componentType = Generics.getTypeParameter(type, Iterable.class.getTypeParameters()[0]);

		if (componentType == null)
		{
			// simple native values will still decode despite unknown type
			componentType = Object.class;
		}

		// TODO this does not respect denormalisation by enhancing existing existing items
		// instead it adds to the existing items
		for (Set<Property> itemProperties : propertySets)
		{
			Object decoded = delegate.decode(itemProperties, path, componentType);

			// if we cannot convert every member of the list we fail
			if (decoded == null)
			{
				throw new IllegalStateException("Could not decode " + path + " to " + componentType);
			}

			if (decoded == NULL_VALUE)
			{
				decoded = null;
			}

			list.add(decoded);
		}

		// convert the List into other Iterable types if needed
		Iterable<?> iterable = datastore.getTypeConverter().convert(list, type);
		
		return iterable;
	}

	public static List<Set<Property>> decodePropertySets(final Set<Property> properties)
	{
		// need to adapt a set of property lists into a list of property sets
		List<Set<Property>> propertySets = Lists.newArrayList();
		boolean complete = false;
		for (int index = 0; !complete; index++)
		{
			complete = true;
			Set<Property> result = new TreeSet<Property>();
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
					throw new IllegalStateException("Expected a List but found " + list);
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
		return propertySets;
	}

	protected Collection<Object> createCollection(Type type)
	{
		// support reusing existing implementations 
		if (datastore.refresh != null)
		{
			@SuppressWarnings("unchecked")
			Collection<Object> result = (Collection<Object>) datastore.refresh;
			datastore.refresh = null;
			return result;
		}
		else
		{
			return new ArrayList<Object>();
		}
	}

	public Set<Property> encode(Object object, Path path, boolean indexed)
	{
		if (object instanceof Iterable<?>)
		{
			Iterable<?> list = (Iterable<?>) object;

			List<Set<Property>> propertySets = new ArrayList<Set<Property>>();

			for (Object item : list)
			{
				if (item != null)
				{
					Set<Property> properties = delegate.encode(item, path, indexed);

					// fail if we cannot encode any of the items
					if (properties == null)
					{
						return null;
					}
					
					propertySets.add(properties);
				}
				else
				{
					propertySets.add(null);
				}
			}
			
			return encodePropertySets(propertySets);
		}
		else
		{
			return null;
		}
	}

	public static Set<Property> encodePropertySets(List<Set<Property>> propertySets)
	{
		final Map<Path, List<Property>> lists = new HashMap<Path, List<Property>>(8);

		int length = 0;
		for (Set<Property> itemProperties : propertySets)
		{
			if (itemProperties != null)
			{
				for (Property property : itemProperties)
				{
					Path itemPath = property.getPath();

					// create or get existing value list
					List<Property> values = lists.get(itemPath);
					if (values == null)
					{
						values = new ArrayList<Property>(4);
						lists.put(itemPath, values);
					}

					// need to pad the list with nulls if any values are missing
					while (values.size() < length)
					{
						values.add(null);
					}
					values.add(property);
				}
			}
			else
			{
				// put a null for every field - could put place holder for null
				Collection<List<Property>> values = lists.values();
				for (List<Property> value : values)
				{
					value.add(null);
				}
			}
			length++;
		}

		// optimise for case of single properties
		if (lists.size() == 1)
		{
			Path childPath = lists.keySet().iterator().next();
			List<Property> properties = lists.get(childPath);
			List<Object> values = new ArrayList<Object>(properties.size());
			boolean indexed = false;
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
			// lazily create properties as needed
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


}
