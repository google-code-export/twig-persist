package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.TypesafeSession;
import com.vercer.engine.persist.util.SimpleProperty;

public class EntityTranslator implements PropertyTranslator
{
	private final TypesafeSession persister;

	public EntityTranslator(TypesafeSession persister)
	{
		this.persister = persister;
	}

	public Object propertiesToTypesafe(Set<Property> fields, Path path, Type type)
	{
		assert fields.size() == 1;
		Key key = (Key) fields.iterator().next().getValue();
		return persister.load(key);
	}

	public Set<Property> typesafeToProperties(final Object instance, Path path, boolean indexed)
	{
		Key key = persister.store(instance);
		Property property = new SimpleProperty(path, key, indexed);
		return Collections.singleton(property);
	}
}
