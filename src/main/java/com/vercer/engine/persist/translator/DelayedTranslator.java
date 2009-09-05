package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.util.reference.ObjectReference;
import com.vercer.util.reference.ReadOnlyObjectReference;

public class DelayedTranslator extends DecoratingTranslator
{

	public DelayedTranslator(PropertyTranslator chained)
	{
		super(chained);
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		return chained.propertiesToTypesafe(properties, path, type);
	}

	public Set<Property> typesafeToProperties(final Object object, final Path path, final boolean indexed)
	{
		ObjectReference<Object> reference = new ReadOnlyObjectReference<Object>()
		{
			public Object get()
			{
				Set<Property> properties = chained.typesafeToProperties(object, path, indexed);
				return properties.iterator().next().getValue();
			}
		};
		Property property = new SimpleProperty(path, reference, indexed);
		return Collections.singleton(property);
	}

}
