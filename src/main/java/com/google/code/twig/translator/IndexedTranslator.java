package com.google.code.twig.translator;

import java.lang.reflect.Type;
import java.util.Set;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;

public abstract class IndexedTranslator implements PropertyTranslator
{
	private PropertyTranslator delegate;

	public Object decode(Set<Property> properties, Path path, Type type)
	{
		return this.delegate.decode(properties, path, type);
	}

	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		return this.delegate.encode(instance, path, isIndexed());
	}

	protected abstract boolean isIndexed();

	public IndexedTranslator(PropertyTranslator delegate)
	{
		this.delegate = delegate;
	}
}
