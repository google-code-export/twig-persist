package com.vercer.engine.persist;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;

public interface StoreCommand
{
	<T> MultipleStoreCommand<T> instances(Collection<T> instances);
	<T> SingleStoreCommand<T> instance(T instance);

	interface CommonStoreCommand<T, C extends CommonStoreCommand<T, C>>
	{
		C parent(Object parent);
		C batch();
		C ensureUniqueKey();
	}

	interface SingleStoreCommand<T> extends CommonStoreCommand<T, SingleStoreCommand<T>>
	{
		Key returnKeyNow();
		Future<Key> returnKeyLater();
	}

	interface MultipleStoreCommand<T> extends CommonStoreCommand<T, MultipleStoreCommand<T>>
	{
		Map<T, Key> returnKeysNow();
		Future<Map<T, Key>> returnKeysLater();
	}
}
