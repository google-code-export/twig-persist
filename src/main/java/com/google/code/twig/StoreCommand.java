package com.google.code.twig;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Key;

public interface StoreCommand
{
	<T> SingleStoreCommand<T, ?> instance(T instance);
	<T> MultipleStoreCommand<T, ?> instances(Collection<? extends T> instances);
	<T> MultipleStoreCommand<T, ?> instances(T... instances);

	interface CommonStoreCommand<T, C extends CommonStoreCommand<T, C>>
	{
		C parent(Object parent);
		C ensureUniqueKey();
	}

	interface SingleStoreCommand<T, C extends SingleStoreCommand<T, C>> extends CommonStoreCommand<T, C>, CommandTerminator<Key>
	{
		C id(long id);
		C id(String id);
	}

	interface MultipleStoreCommand<T, C extends MultipleStoreCommand<T, C>> extends CommonStoreCommand<T, C>, CommandTerminator<Map<T, Key>>
	{
		C ids(String... ids);
		C ids(Long... ids);
		C ids(List<?> ids);
	}
}
