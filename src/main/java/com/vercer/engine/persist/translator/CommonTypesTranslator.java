package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.SimpleProperty;

public class CommonTypesTranslator implements PropertyTranslator
{
	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		if (type == Locale.class)
		{
			String locale = (String) properties.iterator().next().getValue();
			String[] parts = locale.split("_", -1);
			return new Locale(parts[0], parts[1], parts[2]);
		}
		return null;
	}

	public Set<Property> typesafeToProperties(Object object, Path prefix, boolean indexed)
	{
		if (object instanceof Locale)
		{
			Locale locale = (Locale) object;
			String text = locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
			Property property = new SimpleProperty(prefix, text, indexed);
			return Collections.singleton(property);
		}
		return null;
	}

}
