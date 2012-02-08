package com.google.code.twig.translator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.conversion.TypeConverter;
import com.google.code.twig.util.PrefixPropertySet;
import com.google.code.twig.util.PropertyPathComparator;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.SimpleProperty;
import com.google.code.twig.util.collections.MergeSet;
import com.google.code.twig.util.generic.Generics;

/**
 * @author John Patterson <jdpatterson@gmail.com>
 * TODO pass in model meta-data and remove abstract methods
 */
public abstract class FieldTranslator implements PropertyTranslator
{
	private static final Logger log = Logger.getLogger(FieldTranslator.class.getName());
	private static final PropertyPathComparator COMPARATOR = new PropertyPathComparator();
	private final TypeConverter converters;

	public FieldTranslator(TypeConverter converters)
	{
		this.converters = converters;
	}

	public final Object decode(Set<Property> properties, Path path, Type type)
	{
		if (properties.size() == 1)
		{
			Property property = PropertySets.firstProperty(properties);
			if (property.getValue() == null && property.getPath().equals(path))
			{
				return NULL_VALUE;
			}
		}
		
		// create the instance
		Class<?> clazz = Generics.erase(type);
		Object instance = createInstance(clazz);
		
		// ensure the properties are sorted
		if (properties instanceof SortedSet<?> == false)
		{
			Set<Property> sorted = new TreeSet<Property>(COMPARATOR);
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

				decode(instance, field, fieldPath, childProperties);
			}
		}
		
		return instance;
	}

	protected Comparator<Field> getFieldComparator()
	{
		return null;
	}

	protected void decode(Object instance, Field field, Path path, Set<Property> properties)
	{
		// get the correct translator for this field
		PropertyTranslator translator = decoder(field, properties);

		// get the type that we need to store
		Type type = type(field);

		onBeforeDecode(field, properties);

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
		
		// for collections we can reuse an existing instance
		if (reusedExistingImplementation(value, field, instance) == false)
		{
			// the stored type may not be the same as the declared type
			// due to the ability to define what type to store an instance
			// as using FieldTypeStrategy.type(Field) or @Type annotation
			if (type.equals(field.getGenericType()) == false)
			{
				value = converters.convert(value, field.getGenericType());
			}
			
			setFieldValue(instance, field, value);
		}
		
		onAfterDecode(field, value);
	}

	private boolean reusedExistingImplementation(Object value, Field field, Object instance)
	{
		// check for a default implementations of collections and reuse
		if (Collection.class.isAssignableFrom(field.getType()))
		{
			try
			{
				// see if there is a default value
				Collection<?> existing = (Collection<?>) field.get(instance);
				if (existing != null)
				{
					if (value == null)
					{
						// just leave default if we have a null collection
						return true;
					}
					else if (existing.getClass() != value.getClass())
					{
						// make sure the value is a list - could be a blob
						if (!Collection.class.isAssignableFrom(value.getClass()))
						{
							value = converters.convert(value, ArrayList.class);
						}
						
						existing.clear();
						typesafeAddAll((Collection<?>) value, existing);
						return true;
					}
				}
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}
		else if (Map.class.isAssignableFrom(field.getType()))
		{
			try
			{
				// see if there is a default value
				Map<?, ?> existing = (Map<?, ?>) field.get(instance);
				if (existing != null)
				{
					if (value == null)
					{
						return true;
					}
					else if (existing.getClass() != value.getClass())
					{
						// make sure the value is a map - could be a blob
						if (!Map.class.isAssignableFrom(value.getClass()))
						{
							value = converters.convert(value, HashMap.class);
						}
						
						existing.clear();
						typesafePutAll((Map<?, ?>) value, existing);
						return true;
					}
				}
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}
		return false;
	}

	private void setFieldValue(Object instance, Field field, Object value)
	{
		try
		{
			field.set(instance, value);
		}
		catch (Exception e)
		{
			throw new IllegalStateException("Could not set value " + value + " to field " + field, e);
		}
	}

	@SuppressWarnings("unchecked")
	private <K, V> void typesafePutAll(Map<?, ?> value, Map<?, ?> existing)
	{
		((Map<K, V>) existing).putAll((Map<K, V>) value);
	}

	@SuppressWarnings("unchecked")
	private <T> void typesafeAddAll(Collection<?> value, Collection<?> existing)
	{
		((Collection<T>) existing).addAll((Collection<T>) value);
	}


	protected void onAfterDecode(Field field, Object value)
	{
	}

	protected void onBeforeDecode(Field field, Set<Property> childProperties)
	{
	}

	protected String fieldToPartName(Field field)
	{
		return field.getName();
	}

	protected Type type(Field field)
	{
		return field.getType();
	}

	protected Object createInstance(Class<?> clazz)
	{
		try
		{
			Constructor<?> constructor = getNoArgsConstructor(clazz);
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

	protected abstract Constructor<?> getNoArgsConstructor(Class<?> clazz) throws NoSuchMethodException;

	protected abstract Collection<Field> getSortedAccessibleFields(Class<?> clazz);
	
	public final Set<Property> encode(Object object, Path path, boolean indexed)
	{
		onBeforeEncode(path, object);
		if (object == null)
		{
			return Collections.emptySet();
		}

		try
		{
			Collection<Field> fields = getSortedAccessibleFields(object.getClass());
			MergeSet<Property> merged = new MergeSet<Property>(fields.size());
			for (Field field : fields)
			{
				if (stored(field))
				{
					// get the type that we need to store
					Type type = type(field);

					Object value = field.get(object);
					
					// we may need to convert the object if it is not assignable
					value = converters.convert(value, type);

					onBeforeEncode(field, value);
					
					PropertyTranslator translator = encoder(field, value);
					Path childPath = new Path.Builder(path).field(fieldToPartName(field)).build();
					Set<Property> properties = translator.encode(value, childPath, indexed(field));
					
					if (value == NULL_VALUE)
					{
						if (isNullStored())
						{
							merged.add(new SimpleProperty(childPath, null, indexed(field)));
						}
						continue;
					}

					if (properties == null)
					{
						throw new IllegalStateException("Could not translate value to properties: " + value);
					}
					merged.addAll(properties);
					
					onAfterEncode(field, properties);
				}
			}

			onAfterEncode(path, merged);
			
			return merged;
		}
		catch (IllegalAccessException e)
		{
			throw new IllegalStateException(e);
		}
		catch (IllegalArgumentException e)
		{
			if (DataTypeUtils.isSupportedType(object.getClass()))
			{
				throw new IllegalStateException("Native data type " + object.getClass() + " should not be configured as embedded");
			}
			else
			{
				throw e;
			}
		}
	}

	protected void onAfterEncode(Path path, Set<Property> properties)
	{
	}

	protected void onBeforeEncode(Path path, Object object)
	{
	}

	protected void onAfterEncode(Field field, Set<Property> properties)
	{
	}

	protected void onBeforeEncode(Field field, Object value)
	{
	}

	protected abstract boolean isNullStored();

	protected abstract boolean indexed(Field field);

	protected abstract boolean stored(Field field);

	protected abstract PropertyTranslator encoder(Field field, Object instance);
	
	protected abstract PropertyTranslator decoder(Field field, Set<Property> properties);

}
