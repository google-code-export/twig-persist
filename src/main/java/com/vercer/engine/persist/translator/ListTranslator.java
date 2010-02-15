package com.vercer.engine.persist.translator;

import java.io.Serializable;
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

import com.google.appengine.api.datastore.Blob;
import com.google.common.collect.Lists;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.Path.Part;
import com.vercer.engine.persist.conversion.DefaultTypeConverter.BlobToSerializable;
import com.vercer.engine.persist.conversion.DefaultTypeConverter.SerializableToBlob;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.engine.persist.util.SinglePropertySet;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;

public class ListTranslator extends DecoratingTranslator
{
	private static final SerializableToBlob SERIALIZABLE_TO_BLOB = new SerializableToBlob();
	private static final BlobToSerializable BLOB_TO_SERIALIZABLE = new BlobToSerializable();
	private static final String LIST_SERIALIZED_META = "list";

	public ListTranslator(PropertyTranslator chained)
	{
		super(chained);
	}

	public Object propertiesToTypesafe(final Set<Property> properties, final Path path, Type type)
	{
		// only handle lists
		if (!GenericTypeReflector.erase(type).isAssignableFrom(ArrayList.class))
		{
			// pass on all other types down the chain
			return chained.propertiesToTypesafe(properties, path, type);
		}

		// TODO handle this in a more general place
		if (properties.isEmpty())
		{
			return null;
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
				Part nextPart = itemPath.firstPartAfterPrefix(path);
				Object list = property.getValue();
				if (list instanceof List<?>)
				{
					values = (List<?>) list;
				}
				else if (nextPart != null && nextPart.isMeta() && nextPart.getName().equals(LIST_SERIALIZED_META))
				{
					values = (List<?>) BLOB_TO_SERIALIZABLE.convert((Blob) list);
					itemPath = itemPath.head();
				}
				else
				{
					// this is not a list
					return null;
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
		Type exact = GenericTypeReflector.getExactSuperType(type, List.class);
		Type componentType = ((ParameterizedType) exact).getActualTypeArguments()[0];

		// decode each item of the list
		List<Object> objects = new ArrayList<Object>();
		for (Set<Property> itemProperties : propertySets)
		{
			Object convertedChild = chained.propertiesToTypesafe(itemProperties, path, componentType);
			
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
						Object value = property.getValue();
						Path itemPath = property.getPath();
						
						// we can encode only one level of collection
						if (value instanceof List<?>)
						{
							itemPath = new Path.Builder(itemPath).meta(LIST_SERIALIZED_META).build();
							value = SERIALIZABLE_TO_BLOB.convert((Serializable) value);
						}
						
						List<Object> values = lists.get(itemPath);
						if (values == null)
						{
							values = new ArrayList<Object>(4);

							lists.put(itemPath, values);
						}

						// need to pad the list with nulls if any values are missing
						while (values.size() < count)
						{
							values.add(null);
						}
						values.add(value);
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
