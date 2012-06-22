package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.util.PathPrefixPredicate;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.SimpleProperty;
import com.google.code.twig.util.collections.PrependSet;
import com.google.common.base.Predicates;
import com.google.common.collect.Sets;

public class PolymorphicTranslator extends DecoratingTranslator
{
	public static final String CLASS_PROPERTY = "class";
	private final Configuration configuration;

	public PolymorphicTranslator(PropertyTranslator chained, Configuration strategy)
	{
		super(chained);
		this.configuration = strategy;
	}

	public Object decode(Set<Property> properties, final Path prefix, Type type)
	{
		Path kindNamePath = new Path.Builder(prefix).meta(CLASS_PROPERTY).build();
		String kindName = PropertySets.valueForPath(kindNamePath.toString(), properties);

		// there may be no polymorphic field
		if (kindName != null)
		{
			// filter out the class name
			properties = Sets.filter(properties, Predicates.not(new PathPrefixPredicate(kindNamePath)));
			type = configuration.kindToType(kindName);
		}
		return delegate.decode(properties, prefix, type);
	}

	public Set<Property> encode(Object object, Path prefix, boolean indexed)
	{
		Set<Property> properties = delegate.encode(object, prefix, indexed);
		
//		// add the type meta-data if we need to query by sub-type
//		if (configuration.polymorphic(object.getClass()))
//		{
			String kind = configuration.typeToKind(object.getClass());
			Path kindPath = new Path.Builder(prefix).meta(CLASS_PROPERTY).build();
			Property property = new SimpleProperty(kindPath, kind, true);
			
			return new PrependSet<Property>(property, properties);
//		}
//		else
//		{
//			// native types are stored with the same exact type
//			return properties;
//		}
	}

}
