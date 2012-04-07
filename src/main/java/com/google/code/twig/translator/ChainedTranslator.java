package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;

public class ChainedTranslator implements PropertyTranslator
{
	private final List<PropertyTranslator> translators;

	public ChainedTranslator(PropertyTranslator... translators)
	{
		this.translators = new ArrayList<PropertyTranslator>(Arrays.asList(translators));
	}

	public ChainedTranslator()
	{
		this.translators = new ArrayList<PropertyTranslator>(4);
	}

	public PropertyTranslator append(PropertyTranslator translator)
	{
		this.translators.add(translator);
		return this;
	}

	public PropertyTranslator prepend(PropertyTranslator translator)
	{
		this.translators.add(0, translator);
		return this;
	}

	public Iterator<PropertyTranslator> translators()
	{
		return translators.iterator();
	}

	public Set<Property> encode(Object object, Path prefix, boolean indexed)
	{
		if (object == null)
		{
			return PropertySets.singletonPropertySet(prefix, null, indexed);
		}

		try
		{
			for (PropertyTranslator translator : translators)
			{
				Set<Property> result = translator.encode(object, prefix, indexed);
				if (result != null)
				{
					return result;
				}
			}
			return null;
		}
		catch (Throwable t)
		{
			throw new RuntimeException("Problem encoding " + object + " at " + prefix , t);
		}
	}

	public Object decode(Set<Property> properties, Path prefix, Type type)
	{
		for (PropertyTranslator translator : translators)
		{
			Object result = translator.decode(properties, prefix, type);
			if (result != null)
			{
				return result;
			}
		}
		return null;
	}
}