package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;

public class GuiceTranslator implements PropertyTranslator
{
	private final Injector injector;

	public GuiceTranslator(Injector injector)
	{
		this.injector = injector;
	}

	public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		if (intercept(type))
		{
			List<?> bindings = injector.findBindingsByType(TypeLiteral.get(type));
			if (!bindings.isEmpty())
			{
				return injector.getInstance((Class<?>) type);
			}
		}
		return null;
	}

	protected boolean intercept(Type type)
	{
		return true;
	}

	public Set<Property> typesafeToProperties(Object object, Path path, boolean indexed)
	{
		if (intercept(object.getClass()))
		{
			List<?> bindings = injector.findBindingsByType(TypeLiteral.get(object.getClass()));
			if (!bindings.isEmpty())
			{
				// signal that we handled the translation
				return Collections.emptySet();
			}
		}
		return null;
	}

}
