package com.google.code.twig.translator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.appengine.api.datastore.Key;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PrefixPropertySet;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.Reflection;
import com.google.code.twig.util.collections.MergeSet;
import com.google.code.twig.util.generic.Generics;
import com.vercer.convert.GenericType;
import com.vercer.convert.TypeConverter;

/**
 * @author John Patterson <john@vercer.com>
 */
public abstract class FieldTranslator implements PropertyTranslator
{
	private static final Logger log = Logger.getLogger(FieldTranslator.class.getName());
	private final TypeConverter converters;

	public FieldTranslator(TypeConverter converters)
	{
		this.converters = converters;
	}

	public Object decode(Set<Property> properties, Path path, Type type)
	{
		if (properties.size() == 1)
		{
			Property property = PropertySets.firstProperty(properties);
			if (property.getPath().equals(path))
			{
				if (property.getValue() == null)
				{
					return NULL_VALUE;
				}
				else if (property.getValue() instanceof Key)
				{
					// key means a referenced instance - probably a model refactor
					return null;
				}
			}
		}
		
		// create the instance
		Class<?> clazz = Generics.erase(type);
		Object instance = createInstance(clazz);
		
		// sanity check that our instance is the correct type
		assert clazz.isAssignableFrom(instance.getClass());

		// ensure the properties are sorted
		if (properties instanceof SortedSet<?> == false)
		{
			Set<Property> sorted = new TreeSet<Property>();
			sorted.addAll(properties);
			properties = sorted;
		}

		// both fields and properties are sorted by field name
		Collection<Field> fields = getSortedAccessibleFields(instance.getClass());
		Iterator<PrefixPropertySet> ppss = PropertySets.prefixPropertySets(properties, path).iterator();
		PrefixPropertySet pps = null;
		for (Field field : fields)
		{
			if (stored(field))
			{
				String name = fieldToPartName(field);
				Path fieldPath = new Path.Builder(path).field(name).build();

				// handle missing class fields by ignoring the properties
				while (ppss.hasNext())
				{
					if (pps == null)
					{
						pps = ppss.next();
					}

					if (pps.getPrefix().compareTo(fieldPath) < 0)
					{
						log.warning("No field found for properties with prefix " + pps.getPrefix() + " in class " + clazz);

						// get more properties
						pps = null;
					}
					else
					{
						break;
					}
				}

				// if there are no properties for the field we must still
				// run a translator because some translators do not require
				// any properties to set a field value e.g. KeyTranslator
				Set<Property> childProperties;
				if (pps == null || !fieldPath.equals(pps.getPrefix()))
				{
					// there were no properties for this field
					childProperties = Collections.emptySet();
				}
				else
				{
					childProperties = pps.getProperties();

					// indicate we used these properties
					pps = null;
				}

				decodeField(instance, field, fieldPath, childProperties);
			}
		}

		return instance;
	}

	protected Comparator<Field> getFieldComparator()
	{
		return null;
	}

	protected void decodeField(Object instance, Field field, Path path, Set<Property> properties)
	{
		// get the correct translator for this field
		PropertyTranslator translator = decoder(field, properties);

		// get the type that we need to store
		Type type = type(field);
		
		// generic declarations are not captured in java
		if (GenericType.class.isAssignableFrom(Generics.erase(type)))
		{
			type = Generics.getTypeParameter(type, GenericType.class.getTypeParameters()[0]);
		}

		onBeforeDecode(field, instance);
		
		Object value;
		try
		{
			value = translator.decode(properties, path, type);
		}
		catch (Exception e)
		{
			// add a bit of context to the problem
			throw new IllegalStateException("Problem translating field " + field + " with properties " + properties, e);
		}

		if (value == null)
		{
			throw new IllegalStateException("Could not translate path " + path);
		}

		// successfully decoded as a null value
		if (value == NULL_VALUE)
		{
			if (properties.isEmpty())
			{
				// leave default value for fields with no stored properties
				return;
			}
			else
			{
				value = null;
			}
		}
		else
		{
			// the value can be stored as a different type
			value = converters.convert(value, type, field.getGenericType());
		}


		setFieldValue(instance, field, value);

		onAfterDecode(field, value);
	}

	private void setFieldValue(Object instance, Field field, Object value)
	{
		try
		{
			Reflection.set(field, instance, value);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Could not set value " + value + " to field " + field, e);
		}
	}

	protected void onAfterDecode(Field field, Object value)
	{
	}

	protected void onBeforeDecode(Field field, Object instance)
	{
	}

	protected String fieldToPartName(Field field)
	{
		return field.getName();
	}

	protected Type type(Field field)
	{
		return field.getGenericType();
	}

	protected Object createInstance(Class<?> clazz)
	{
		try
		{
			Constructor<?> constructor = getDefaultConstructor(clazz);
			return constructor.newInstance();
		}
		catch (NoSuchMethodException e)
		{
			throw new IllegalArgumentException("Could not find no args constructor in " + clazz, e);
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Could not construct instance of " + clazz, e);
		}
	}

	protected abstract Constructor<?> getDefaultConstructor(Class<?> clazz) throws NoSuchMethodException;

	protected abstract Collection<Field> getSortedAccessibleFields(Class<?> clazz);

	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		if (instance == null)
		{
			return Collections.emptySet();
		}

		try
		{
			Collection<Field> fields = getSortedAccessibleFields(instance.getClass());
			MergeSet<Property> merged = new MergeSet<Property>(fields.size());
			for (Field field : fields)
			{
				if (stored(field))
				{
					// get the type that we need to store
					Type type = type(field);

					if (GenericType.class.isAssignableFrom(Generics.erase(type)))
					{
						type = Generics.getTypeParameter(type, GenericType.class.getTypeParameters()[0]);
					}

					Object value = Reflection.get(field, instance);

					Path childPath = new Path.Builder(path).field(fieldToPartName(field)).build();

					Set<Property> encoded;
					if (value != null)
					{
						// convert the object if a type was configured
						if (!type.equals(field.getGenericType()))
						{
							// field might be a generic parameter if declared in super type
							Type from = Generics.getExactFieldType(field, instance.getClass());

							value = converters.convert(value, from, type);
						}

						PropertyTranslator translator = encoder(field, instance);
						encoded = translator.encode(value, childPath, indexed(field));
						if (encoded == null)
						{
							throw new IllegalStateException("Could not translate value to properties: " + value);
						}
					}
					else if (indexed(field))
					{
						// only store null if it is indexed
						encoded = PropertySets.singletonPropertySet(childPath, null, true);
					}
					else
					{
						// do not store unindexed null values
						encoded = Collections.emptySet();
					}

					merged.addAll(encoded);
				}
			}

			return merged;
		}
		catch (IllegalArgumentException e)
		{
			if (DataTypeUtils.isSupportedType(instance.getClass()))
			{
				throw new IllegalStateException("Native data type " + instance.getClass() + " should not be configured as embedded", e);
			}
			else
			{
				throw e;
			}
		}
	}

	protected abstract boolean indexed(Field field);

	protected abstract boolean stored(Field field);

	protected abstract PropertyTranslator encoder(Field field, Object instance);

	protected abstract PropertyTranslator decoder(Field field, Set<Property> properties);

}
