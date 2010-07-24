package com.vercer.engine.persist.standard;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.vercer.engine.persist.LoadCommand.SingleTypedLoadCommand;

class StandardSingleTypedLoadCommand<T> extends StandardCommonTypedLoadCommand<T, StandardSingleTypedLoadCommand<T>> implements SingleTypedLoadCommand<T, StandardSingleTypedLoadCommand<T>>
{
	private final Object id;
	private final StandardTypedLoadCommand<T> command;

	StandardSingleTypedLoadCommand(StandardTypedLoadCommand<T> command, Object id)
	{
		super(command.datastore);
		this.command = command;
		this.id = id;
	}

	@Override
	public Future<T> returnResultLater()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T returnResultNow()
	{
		// the stored type of the id can be defined for the id field
		Field keyField = datastore.keyField(command.type);
		
		Type keyType;
		if (keyField != null)
		{
			keyType = datastore.fieldStrategy.typeOf(keyField);
		}
		else
		{
			// no key field so id must have been set explicitly when stored
			assert id instanceof Long || id instanceof String;
			keyType = id.getClass();
		}
		
		// convert the id to the same type as was stored
		Object converted = datastore.converter.convert(id, keyType);

		Key key;
		String kind = datastore.fieldStrategy.typeToKind(command.type);
		
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
			String keyName = datastore.converter.convert(converted, String.class);

			if (parentKey == null)
			{
				key = KeyFactory.createKey(kind, keyName);
			}
			else
			{
				key = KeyFactory.createKey(parentKey, kind, keyName);
			}
		}
		return keyToInstance(key, propertyRestriction);
	}
}
