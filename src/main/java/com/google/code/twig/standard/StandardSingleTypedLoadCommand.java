package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LoadCommand.SingleTypedLoadCommand;

public class StandardSingleTypedLoadCommand<T> extends StandardCommonLoadCommand<StandardSingleTypedLoadCommand<T>> implements SingleTypedLoadCommand<T>
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
		Field keyField = datastore.idField(command.type);
		String kind = datastore.getConfiguration().typeToKind(command.type);
		Key key = idToKey(id, keyField, kind, datastore, parentKey);
		
		@SuppressWarnings("unchecked")
		T keyToInstance = (T) keyToInstance(key, propertyRestriction);
		return keyToInstance;
	}
}
