package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LoadCommand.MultipleTypedLoadCommand;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.Maps;

public class StandardMultipleTypedLoadCommand<T> 
	extends StandardCommonLoadCommand<StandardMultipleTypedLoadCommand<T>>
	implements MultipleTypedLoadCommand<T>
{
	private final Collection<?> ids;

	StandardMultipleTypedLoadCommand(StandardTypedLoadCommand<T> command, Collection<?> ids, int initialActivationDepth)
	{
		super(command, initialActivationDepth);
		this.ids = ids;
	}

	@Override
	public Future<Map<Object, T>> later()
	{
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Map<Object, T> now()
	{
		// the stored type of the id can be defined for the id field
		Field keyField = datastore.idField(command.type);
		String kind = datastore.getConfiguration().typeToKind(command.type);
		Map<Object, Key> idsToKeys = new LinkedHashMap<Object, Key>(ids.size());
		for (Object id : ids)
		{
			idsToKeys.put(id, idToKey(id, keyField, kind, datastore, parentKey));
		}
		final Map<Key, T> keysToInstances = keysToInstances(idsToKeys.values(), propertyRestriction);
		
		// result will have same ordering as ids
		Map<Object, T> result = Maps.transformValues(idsToKeys, new Function<Key, T>()
		{
			@Override
			public T apply(Key key)
			{
				return keysToInstances.get(key);
			}
		});
		
		// filter out null results
		return Maps.filterValues(result, Predicates.notNull());
	}
}
