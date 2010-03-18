package com.vercer.engine.persist;

public interface LoadCommand
{
	interface TypedLoadCommand<T, C extends TypedLoadCommand<T, C>>
	{
		C withKey(Object key);
	}

	interface SingleLoadCommand
	{
	}

//	<T> TypedLoadCommand<T, C extends TypedLoadCommand<T, C>> type(Class<T> type);
}
