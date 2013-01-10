package com.google.code.twig.standard;

import java.util.Collection;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.code.twig.Property;
import com.google.code.twig.util.Entities;
import com.google.code.twig.util.reference.ObjectReference;

public class StandardEncodeCommand extends StandardCommand
{
	StandardEncodeCommand(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}

	final Entity createEntity()
	{
		if (datastore.encodeKeyDetails.isComplete())
		{
			// we have a complete key with id specified 
			Key key = datastore.encodeKeyDetails.toKey();
			
			return new Entity(key);
		}
		else
		{
			// we have no id specified so must create entity for auto-generated id
			ObjectReference<Key> parentKeyReference = datastore.encodeKeyDetails.getParentKeyReference();
			Key parentKey = parentKeyReference == null ? null : parentKeyReference.get();
			return Entities.createEntity(datastore.encodeKeyDetails.getKind(), null, parentKey);
		}
	}

	final void transferProperties(Entity entity, Collection<Property> properties)
	{
		for (Property property : properties)
		{
			// dereference object references
			Object value = property.getValue();
			
			try
			{
				value = dereferencePropertyValue(value);
			}
			catch (Throwable t)
			{
				throw new RuntimeException("Problem de-referencing property " + property, t);
			}
			
			String name = property.getPath().toString();

			assert !entity.hasProperty(name);

			if (property.isIndexed())
			{
				entity.setProperty(name, value);
			}
			else
			{
				entity.setUnindexedProperty(name, value);
			}
		}
	}

	final Object dereferencePropertyValue(Object value)
	{
		if (value instanceof ObjectReference<?>)
		{
			// single and multiple references use a single value
			value = ((ObjectReference<?>)value).get();
		}
		else if (value instanceof List<?>)
		{
			// embedded collections can return a list of keys
			@SuppressWarnings("unchecked")
			List<Object> values = (List<Object>) value;
			for (int i = 0; i < values.size(); i++)
			{
				Object item = values.get(i);
				if (item instanceof ObjectReference<?>)
				{
					// dereference the value and set it in-place
					Object dereferenced = ((ObjectReference<?>) item).get();
					values.set(i, dereferenced);  // replace the reference
				}
			}
		}
		return value;
	}
}
