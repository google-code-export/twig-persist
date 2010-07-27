package com.google.code.twig;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;

public interface StoreCommand
{
	<T> SingleStoreCommand<T, ?> instance(T instance);
	<T> MultipleStoreCommand<T, ?> instances(Collection<T> instances);
	<T> MultipleStoreCommand<T, ?> instances(T... instances);

	interface CommonStoreCommand<T, C extends CommonStoreCommand<T, C>>
	{
		C parent(Object parent);
		C batch();
		C ensureUniqueKey();
	}

	interface SingleStoreCommand<T, C extends SingleStoreCommand<T, C>> extends CommonStoreCommand<T, C>
	{
		C id(long id);
		C id(String id);
		Key returnKeyNow();
		Future<Key> returnKeyLater();
	}

	interface MultipleStoreCommand<T, C extends MultipleStoreCommand<T, C>> extends CommonStoreCommand<T, C>
	{
		C ids(String... ids);
		C ids(Long... ids);
		C ids(List<?> ids);
		Map<T, Key> returnKeysNow();
		Future<Map<T, Key>> returnKeysLater();
	}
}
