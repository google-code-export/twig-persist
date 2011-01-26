package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.code.twig.LoadCommand;
import com.google.code.twig.Property;
import com.google.code.twig.Restriction;

class StandardCommonLoadCommand<C extends StandardCommonLoadCommand<C>> extends StandardDecodeCommand implements LoadCommand.CommonLoadCommand<C>
{
	final StandardTypedLoadCommand<?> command;
	Restriction<Entity> entityRestriction;
	Restriction<Property> propertyRestriction;
	Key parentKey;

	StandardCommonLoadCommand(StandardTypedLoadCommand<?> command)
	{
		super(command.datastore);
		this.command = command;
	}

	@SuppressWarnings("unchecked")
	@Override
	public C restrictEntities(Restriction<Entity> restriction)
	{
		this.entityRestriction = restriction;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public C restrictProperties(Restriction<Property> restriction)
	{
		this.propertyRestriction = restriction;
		return (C) this;
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

	Key idToKey(Object id, Field keyField, String kind)
	{
		Type keyType;
		if (keyField != null)
		{
			keyType = datastore.getConfiguration().typeOf(keyField);
			id = datastore.getConverter().convert(id, keyType);
		}
		else
		{
			// no key field so id must have been set explicitly when stored
			if (id instanceof Long == false && id instanceof String == false)
			{
				throw new IllegalArgumentException("Id must be String or Long but was " + id.getClass());
			}
		}
		
		// convert the id to the same type as was stored

		Key key;
		
		// the key name is not stored in the fields but only in key
		if (id instanceof Number)
		{
			// only set the id if it is not 0 otherwise auto-generate
			long longValue = ((Number) id).longValue();
			if (parentKey == null)
			{
				key = KeyFactory.createKey(kind, longValue);
			}
			else
			{
				key = KeyFactory.createKey(parentKey, kind, longValue);
			}
		}
		else
		{
			// make into string
			String keyName = datastore.getConverter().convert(id, String.class);

			if (parentKey == null)
			{
				key = KeyFactory.createKey(kind, keyName);
			}
			else
			{
				key = KeyFactory.createKey(parentKey, kind, keyName);
			}
		}
		return key;
	}
}
