package com.google.code.twig.standard;

import java.util.Arrays;
import java.util.Collection;

import com.google.code.twig.LoadCommand.TypedLoadCommand;

public class StandardTypedLoadCommand<T> extends StandardDecodeCommand implements TypedLoadCommand<T>
{
	final Class<? extends T> type;

	public StandardTypedLoadCommand(StrategyObjectDatastore datastore, Class<? extends T> type)
	{
		super(datastore);
		this.type = type;
	}

	@Override
	public StandardSingleTypedLoadCommand<T> id(Object id)
	{
		return new StandardSingleTypedLoadCommand<T>(this, id);
	}

	@Override
	public <I> StandardMultipleTypedLoadCommand<T, I> ids(Collection<? extends I> ids)
	{
		return new StandardMultipleTypedLoadCommand<T, I>(this, ids);
	}

	@Override
	public <K> StandardMultipleTypedLoadCommand<T, K> ids(K... ids)
	{
		return ids(Arrays.asList(ids));
	}
}
