package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

class StandardCommonLoadCommand<C extends StandardCommonLoadCommand<C>> extends StandardDecodeCommand<C>
{
	final StandardTypedLoadCommand<?> command;
	Key parentKey;

	StandardCommonLoadCommand(StandardTypedLoadCommand<?> command)
	{
		super(command.datastore);
		this.command = command;
	}

	@SuppressWarnings("unchecked")
	public final C parent(Object parent)
	{
		assert parent != null;
		parentKey = datastore.associatedKey(parent);
		if (parentKey == null)
		{
			throw new IllegalArgumentException("Parent is not associated: " + parent);
		}
		return (C) this;
	}
	
	static Key idToKey(Object id, Field field, String kind, TranslatorObjectDatastore datastore, Key parentKey)
	{
		if (field != null)
		{
			Type storeType = datastore.getConfiguration().typeOf(field);
			if (storeType != field.getGenericType())
			{
				id = datastore.getConverter().convert(id, field.getGenericType(), storeType);
			}
		}
		else
		{
			// no key field so id must have been set explicitly when stored
			if (id instanceof Long == false && id instanceof String == false)
			{
				throw new IllegalArgumentException("Explicit id must be String or Long but was " + id.getClass());
			}
		}
		
		// the key name is not stored in the fields but only in key
		if (id instanceof Number)
		{
			// only set the id if it is not 0 otherwise auto-generate
			long longValue = ((Number) id).longValue();
			if (parentKey == null)
			{
				return KeyFactory.createKey(kind, longValue);
			}
			else
			{
				return KeyFactory.createKey(parentKey, kind, longValue);
			}
		}
		else if (id instanceof String)
		{
			String name = (String) id;
			if (parentKey == null)
			{
				return KeyFactory.createKey(kind, name);
			}
			else
			{
				return KeyFactory.createKey(parentKey, kind, name);
			}
		}
		else
		{
			throw new IllegalArgumentException("@Id field must be stored as a " +
					"Number or String but was " + id.getClass() + ". Use @Type " +
					"to define the stored type and configure a type converter which" +
					"can handle the conversion in both directions.");
		}
	}
}
