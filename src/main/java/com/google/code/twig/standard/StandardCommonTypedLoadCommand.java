package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.code.twig.LoadCommand.CommonTypedLoadCommand;
import com.google.code.twig.Property;
import com.google.code.twig.Restriction;

class StandardCommonTypedLoadCommand<T, C extends StandardCommonTypedLoadCommand<T, C>> extends StandardDecodeCommand implements CommonTypedLoadCommand<T, C>
{
	final StandardTypedLoadCommand<T> command;
	Restriction<Entity> entityRestriction;
	Restriction<Property> propertyRestriction;
	Key parentKey;

	StandardCommonTypedLoadCommand(StandardTypedLoadCommand<T> command)
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
			keyType = datastore.getFieldStrategy().typeOf(keyField);
		}
		else
		{
			// no key field so id must have been set explicitly when stored
			assert id instanceof Long || id instanceof String;
			keyType = id.getClass();
		}
		
		// convert the id to the same type as was stored
		Object converted = datastore.getConverter().convert(id, keyType);

		Key key;
		
		// the key name is not stored in the fields but only in key
		if (converted instanceof Number)
		{
			// only set the id if it is not 0 otherwise auto-generate
			long longValue = ((Number) converted).longValue();
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
			String keyName = datastore.getConverter().convert(converted, String.class);

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
