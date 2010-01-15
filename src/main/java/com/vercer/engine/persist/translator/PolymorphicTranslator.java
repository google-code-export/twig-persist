package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PathPrefixPredicate;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.util.collections.AppendingSet;

public class PolymorphicTranslator extends DecoratingTranslator
{
	private static final String CLASS_NAME = "class";

	public PolymorphicTranslator(PropertyTranslator chained)
	{
		super(chained);
	}

	public Object propertiesToTypesafe(Set<Property> properties, final Path prefix, Type type)
	{
		String typeName = null;
		Path typeNamePath = new Path.Builder(prefix).meta(CLASS_NAME).build();
		for (Property property : properties)
		{
			if (property.getPath().equals(typeNamePath))
			{
				typeName = (String) property.getValue();
				break;
			}
		}

		// filter out the class name
		Set<Property> filtered = Sets.filter(properties,
				Predicates.not(new PathPrefixPredicate(typeNamePath)));
		filtered = new HashSet<Property>(filtered);
		try
		{
			Class<?> clazz = Class.forName(typeName);
			return chained.propertiesToTypesafe(filtered, prefix, clazz);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException(e);
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

		String className = object.getClass().getName();
		Path classNamePath = new Path.Builder(prefix).meta(CLASS_NAME).build();
		Property property = new SimpleProperty(classNamePath, className, true);

		return new AppendingSet<Property>(property, properties);
	}

}
