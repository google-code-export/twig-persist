package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Currency;
import java.util.Locale;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.engine.persist.util.SinglePropertySet;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;

public class CoreTypesTranslator implements PropertyTranslator
{
	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		Class<?> erased = GenericTypeReflector.erase(type);
		if (erased == Locale.class)
		{
			String locale = PropertySets.firstValue(properties);
			String[] parts = locale.split("_", -1);
			return new Locale(parts[0], parts[1], parts[2]);
		}
		else if (Class.class.isAssignableFrom(erased))
		{
			String name = PropertySets.firstValue(properties);
			try
			{
				return Class.forName(name);
			}
			catch (ClassNotFoundException e)
			{
				throw new IllegalStateException(e);
			}
		}
		else if (Currency.class == erased)
		{
			String name = PropertySets.firstValue(properties);
			return Currency.getInstance(name);
		}
		else if (URI.class == erased)
		{
			String name = PropertySets.firstValue(properties);
			try
			{
				return new URI(name);
			}
			catch (URISyntaxException e)
			{
				throw new IllegalStateException(e);
			}
		}
		else if (URL.class == erased)
		{
			String name = PropertySets.firstValue(properties);
			try
			{
				return new URL(name);
			}
			catch (MalformedURLException e)
			{
				throw new IllegalStateException(e);
			}
		}
		return null;
	}

	public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
	{
		if (instance instanceof Locale)
		{
			Locale locale = (Locale) instance;
			String text = locale.getLanguage() + "_" + locale.getCountry() + "_" + locale.getVariant();
			return new SinglePropertySet(path, text, indexed);
		}
		else if (instance instanceof Class<?>)
		{
			String name = ((Class<?>) instance).getName();
			return new SinglePropertySet(path, name, indexed);
		}
		else if (instance instanceof Currency)
		{
			String name = ((Currency) instance).getCurrencyCode();
			return new SinglePropertySet(path, name, indexed);
		}
		else if (instance instanceof URI)
		{
			String name = ((URI) instance).toString();
			return new SinglePropertySet(path, name, indexed);
		}
		else if (instance instanceof URL)
		{
			String name = ((URL) instance).toString();
			return new SinglePropertySet(path, name, indexed);
		}
		return null;
	}

}
