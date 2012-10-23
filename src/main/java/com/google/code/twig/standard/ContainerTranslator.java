package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;

public class ContainerTranslator implements PropertyTranslator
{
	private final TranslatorObjectDatastore datastore;

	public ContainerTranslator(TranslatorObjectDatastore datastore)
	{
		this.datastore = datastore;
	}
	
	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		return datastore.associatedInstance(datastore.decodeKey);
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		return Collections.emptySet();
	}

}
