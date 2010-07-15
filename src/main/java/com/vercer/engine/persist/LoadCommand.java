package com.vercer.engine.persist;

import java.util.Map;
import java.util.Collection;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;

public interface LoadCommand
{
	interface TypedLoadCommand<T>
	{
		SingleTypedLoadCommand<T> id(Object id);
		<K> MultipleTypedLoadCommand<T, K> ids(Collection<? extends K> ids);
		<K> MultipleTypedLoadCommand<T, K> ids(K... ids);
	}

	interface BaseTypedLoadCommand<T, C extends BaseTypedLoadCommand<T, C>>
	{
		C restrictEntities(Restriction<Entity> restriction);
		C restrictProperties(Restriction<Property> restriction);
	}
	
	interface SingleTypedLoadCommand<T> extends BaseTypedLoadCommand<T, SingleTypedLoadCommand<T>>
	{
		T returnResultNow();
		Future<T> returnResultLater();
	}
	
	interface MultipleTypedLoadCommand<T, K> extends BaseTypedLoadCommand<T, MultipleTypedLoadCommand<T, K>>
	{
		Map<? super K, ? super T> returnResultsNow();
		Future<Map<? super K, ? super T>> returnResultsLater();
	}

	<T> TypedLoadCommand<T> type(Class<T> type);
}
