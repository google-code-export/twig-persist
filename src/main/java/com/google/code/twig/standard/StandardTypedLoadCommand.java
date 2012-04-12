package com.google.code.twig.standard;

import java.util.Collection;

import com.google.code.twig.LoadCommand.TypedLoadCommand;

public class StandardTypedLoadCommand<T> implements TypedLoadCommand<T>
{
	final Class<? extends T> type;
	final TranslatorObjectDatastore datastore;

	public StandardTypedLoadCommand(TranslatorObjectDatastore datastore, Class<? extends T> type)
	{
		this.datastore = datastore;
		this.type = type;
	}

	@Override
	public StandardSingleTypedLoadCommand<T> id(Object id)
	{
		return new StandardSingleTypedLoadCommand<T>(this, id, datastore.defaultActivationDepth);
	}

	@Override
	public StandardMultipleTypedLoadCommand<T> ids(Collection<?> ids)
	{
		return new StandardMultipleTypedLoadCommand<T>(this, ids, datastore.defaultActivationDepth);
	}
}
