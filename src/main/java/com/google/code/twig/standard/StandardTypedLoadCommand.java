package com.vercer.engine.persist.standard;

import java.util.Arrays;
import java.util.Collection;

import com.vercer.engine.persist.LoadCommand.MultipleTypedLoadCommand;
import com.vercer.engine.persist.LoadCommand.TypedLoadCommand;

public class StandardTypedLoadCommand<T> extends StandardDecodeCommand implements TypedLoadCommand<T>
{
	final Class<T> type;

	public StandardTypedLoadCommand(StrategyObjectDatastore datastore, Class<T> type)
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
	public <I> StandardMultipleTypedLoadCommand<T, I> ids(Collection<I> ids)
	{
		return new StandardMultipleTypedLoadCommand<T, I>(this, ids);
	}

	@Override
	public <K> StandardMultipleTypedLoadCommand<T, K> ids(K... ids)
	{
		return ids(Arrays.asList(ids));
	}
}
