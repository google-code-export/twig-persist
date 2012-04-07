package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;

public class NoOpTranslator implements PropertyTranslator
{

	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		// TODO Auto-generated method stub
		return null;
	}

}
