package com.vercer.engine.persist.translator;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.conversion.TypeConverters;
import com.vercer.engine.persist.util.PrefixFilteringPropertySet;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.util.Reflection;
import com.vercer.util.collections.MergeSet;

public abstract class ObjectFieldTranslator implements PropertyTranslator
{
	private final TypeConverters converters;

	public ObjectFieldTranslator(TypeConverters converters)
	{
		this.converters = converters;
	}

	public final Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
			Class<?> clazz = GenericTypeReflector.erase(type);
			Object instance = createInstance(clazz);
			onInstanceCreated(instance);
			activate(properties, clazz, instance, path);
			return instance;
	}

	protected void onInstanceCreated(Object instance)
	{
	}

	private void activate(Set<Property> properties, Class<?> clazz, Object instance, Path path)
	{
		List<Field> fields = Reflection.getAccessibleFields(clazz);
		for (Field field : fields)
		{
			if (stored(field))
			{
				Object value = memberFromProperties(properties, field, path);
				if (value != null)
				{
					// if the value was converted to another type we may need to convert it back
					if (!GenericTypeReflector.isSuperType(field.getGenericType(), value.getClass()))
					{
						value = converters.convert(value, field.getGenericType());
					}

					try
					{
						field.set(instance, value);
					}
					catch (Exception e)
					{
						throw new IllegalStateException("Could not set value " + value + " to field " + field, e);
					}
				}
			}
		}
	}

	protected Object memberFromProperties(Set<Property> properties, Field field, Path path)
	{
		String name = fieldName(field);
		Path childPath = new Path(name);
		Set<Property> fieldProperties = new PrefixFilteringPropertySet(childPath, properties);
		PropertyTranslator translator = translator(field);
		return translator.propertiesToTypesafe(fieldProperties, childPath, typeFromField(field));
	}

	protected String fieldName(Field field)
	{
		return field.getName();
	}

	protected Type typeFromField(Field field)
	{
		return field.getType();
	}

	protected Object createInstance(Class<?> clazz)
	{
		try
		{
		// use no-args constructor
		Constructor<?> constructor = clazz.getDeclaredConstructor();

		// allow access to private constructor
		if (!constructor.isAccessible())
		{
			constructor.setAccessible(true);
		}
		return constructor.newInstance();
		}
		catch (Exception e)
		{
			throw new IllegalArgumentException("Could not construct instance of: " + clazz, e);
		}
	}

	public final Set<Property> typesafeToProperties(Object object, Path path, boolean indexed)
	{
		try
		{
			List<Field> fields = Reflection.getAccessibleFields(object.getClass());
			MergeSet<Property> merged = new MergeSet<Property>(fields.size());
			for (Field field : fields)
			{
				// never store transient fields
				if (!Modifier.isTransient(field.getModifiers()) && stored(field))
				{
					// get the type that we need to store
					Type type = typeFromField(field);

					// we may need to convert the object if it is not assignable
					Object value = field.get(object);
					if (value == null)
					{
						if (isNullStored())
						{
							merged.add(new SimpleProperty(path, null, indexed));
						}
						continue;
					}

					if (!GenericTypeReflector.isSuperType(type, value.getClass()))
					{
						value = converters.convert(value, type);
					}

					Path childPath = new Path(fieldName(field));

					PropertyTranslator translator = translator(field);
					Set<Property> properties = translator.typesafeToProperties(value, childPath, indexed(field));

					merged.addAll(properties);
				}
			}

			return merged;
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	protected boolean isNullStored()
	{
		return false;
	}

	protected abstract boolean indexed(Field field);
	protected abstract boolean stored(Field field);

	protected abstract PropertyTranslator translator(Field field);


}
