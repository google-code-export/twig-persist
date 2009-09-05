package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.util.collections.AppendingSet;

public class PolymorphicTranslator extends DecoratingTranslator
{
	private static final String CLASS_NAME = "$class";

	public PolymorphicTranslator(PropertyTranslator chained)
	{
		super(chained);
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		if (properties instanceof AppendingSet<?>)
		{
			AppendingSet<Property> appending = (AppendingSet<Property>) properties;
			String name = (String) appending.getItem().getValue();
			try
			{
				Class<?> clazz = Class.forName(name);
				return chained.propertiesToTypesafe(properties, prefix, clazz);
			}
			catch (ClassNotFoundException e)
			{
				throw new IllegalStateException(e);
			}
		}
		else
		{
			throw new IllegalStateException("Unexpected set type");
		}
	}
//
//	protected Type className(Set<Property> properties, Path prefix)
//	{
//		Path classNamePath = new Path.Builder(prefix).field(CLASS_NAME).build();
//		for (Property property : properties)
//		{
//			if (property.getPath().equals(classNamePath))
//			{
//				String className = (String) property.getValue();
//				try
//				{
//					return Class.forName(className);
//				}
//				catch (ClassNotFoundException e)
//				{
//					throw new IllegalStateException(e);
//				}
//			}
//		}
//		throw new IllegalStateException("Could not find class name");
//	}

	public Set<Property> typesafeToProperties(Object object, Path prefix, boolean indexed)
	{
		Set<Property> properties = chained.typesafeToProperties(object, prefix, indexed);
		String className = object.getClass().getCanonicalName();
		Path classNamePath = new Path.Builder(prefix).field(CLASS_NAME).build();
		Property property = new SimpleProperty(classNamePath, className, true);
		
		return new AppendingSet<Property>(property, properties);
	}

}
