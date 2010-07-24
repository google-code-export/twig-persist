package com.vercer.engine.persist.standard;

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
	public <K> MultipleTypedLoadCommand<T, K> ids(Collection<K> ids)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <K> MultipleTypedLoadCommand<T, K> ids(K... ids)
	{
		// TODO Auto-generated method stub
		return null;
	}
//	
//	protected final <T> T internalLoad(Class<T> type, Object id)
//	{
//		assert activationDepthDeque.size() == 1;
//
//		Object converted;
//		if (Number.class.isAssignableFrom(id.getClass()))
//		{
//			converted = converter.convert(id, Long.class);
//		}
//		else
//		{
//			converted = converter.convert(id, String.class);
//		}
//		String kind = fieldStrategy.typeToKind(type);
//
//		Key key;
//		if (converted instanceof Long)
//		{
//			key = KeyFactory.createKey(kind, (Long) converted);
//		}
//		else
//		{
//			key = KeyFactory.createKey(kind, (String) converted);
//		}
//
//		// needed to avoid sun generics bug "no unique maximal instance exists..."
//		@SuppressWarnings("unchecked")
//		T result = (T) keyToInstance(key, null);
//		return result;
//	}
}
