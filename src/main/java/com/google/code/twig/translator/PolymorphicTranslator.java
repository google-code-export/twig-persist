package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.strategy.FieldStrategy;
import com.google.code.twig.util.PathPrefixPredicate;
import com.google.code.twig.util.SimpleProperty;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;
import com.vercer.util.collections.PrependSet;

public class PolymorphicTranslator extends DecoratingTranslator
{
	private static final String CLASS_NAME = "class";
	private final FieldStrategy strategy;

	public PolymorphicTranslator(PropertyTranslator chained, FieldStrategy strategy)
	{
		super(chained);
		this.strategy = strategy;
	}

	public Object propertiesToTypesafe(Set<Property> properties, final Path prefix, Type type)
	{
		String kindName = null;
		Path kindNamePath = new Path.Builder(prefix).meta(CLASS_NAME).build();
		for (Property property : properties)
		{
			if (property.getPath().equals(kindNamePath))
			{
				kindName = (String) property.getValue();
				break;
			}
		}

		// there may be no polymorphic field - just use the raw type
		if (kindName != null)
		{
			// filter out the class name
			properties = Sets.filter(properties,
					Predicates.not(new PathPrefixPredicate(kindNamePath)));
			type = strategy.kindToType(kindName);
		}

		return chained.propertiesToTypesafe(properties, prefix, type);
	}

	public Set<Property> typesafeToProperties(Object object, Path prefix, boolean indexed)
	{
		Set<Property> properties = chained.typesafeToProperties(object, prefix, indexed);
		
		// only add the type meta data for non-native types
		if (!DataTypeUtils.isSupportedType(object.getClass()))
		{
			String className = strategy.typeToKind(object.getClass());
			Path classNamePath = new Path.Builder(prefix).meta(CLASS_NAME).build();
			Property property = new SimpleProperty(classNamePath, className, true);
			
			return new PrependSet<Property>(property, properties);
		}
		else
		{
			// native types are stored with the same exact type
			return properties;
		}
	}

}
