package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;

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

	public Set<Property> typesafeToProperties(Object object, Path prefix, boolean indexed)
	{
		for (PropertyTranslator translator : translators)
		{
			Set<Property> result = translator.typesafeToProperties(object, prefix, indexed);
			if (result != null)
			{
				return result;
			}
		}
		return null;
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
	{
		for (PropertyTranslator translator: translators)
		{
			Object result = translator.propertiesToTypesafe(properties, prefix, type);
			if (result != null)
			{
				return result;
			}
		}
		return null;
	}
}