package com.vercer.engine.persist;

import java.util.Map;
import java.util.Collection;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;

public interface LoadCommand
{
	interface TypedLoadCommand<T>
	{
		SingleTypedLoadCommand<T, ?> id(Object id);
		<K> MultipleTypedLoadCommand<T, K> ids(Collection<K> ids);
		<K> MultipleTypedLoadCommand<T, K> ids(K... ids);
	}

	interface CommonTypedLoadCommand<T, C extends CommonTypedLoadCommand<T, C>>
	{
		C restrictEntities(Restriction<Entity> restriction);
		C restrictProperties(Restriction<Property> restriction);
		C parent(Object parent);
	}
	
	interface SingleTypedLoadCommand<T, C extends SingleTypedLoadCommand<T, C>> extends CommonTypedLoadCommand<T, C>
	{
		T returnResultNow();
		Future<T> returnResultLater();
	}
	
	interface MultipleTypedLoadCommand<T, K> extends CommonTypedLoadCommand<T, MultipleTypedLoadCommand<T, K>>
	{
		Map<K, T> returnResultsNow();
		Future<Map<K, T>> returnResultsLater();
	}

	<T> TypedLoadCommand<T> type(Class<T> type);
}
