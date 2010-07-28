package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LoadCommand.SingleTypedLoadCommand;

class StandardSingleTypedLoadCommand<T> extends StandardCommonTypedLoadCommand<T, StandardSingleTypedLoadCommand<T>> implements SingleTypedLoadCommand<T, StandardSingleTypedLoadCommand<T>>
{
	private final Object id;

	StandardSingleTypedLoadCommand(StandardTypedLoadCommand<T> command, Object id)
	{
		super(command);
		this.id = id;
	}

	@Override
	public Future<T> later()
	{
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public T now()
	{
		// the stored type of the id can be defined for the id field
		Field keyField = datastore.keyField(command.type);
		String kind = datastore.fieldStrategy.typeToKind(command.type);
		Key key = idToKey(id, keyField, kind);
		return keyToInstance(key, propertyRestriction);
	}
}
