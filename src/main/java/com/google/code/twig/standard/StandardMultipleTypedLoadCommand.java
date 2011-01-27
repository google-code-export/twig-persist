package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LoadCommand.MultipleTypedLoadCommand;
import com.google.common.base.Function;
import com.google.common.collect.Maps;

public class StandardMultipleTypedLoadCommand<T, I> 
	extends StandardCommonLoadCommand<StandardMultipleTypedLoadCommand<T, I>>
	implements MultipleTypedLoadCommand<T, I, StandardMultipleTypedLoadCommand<T, I>>
{
	private final Collection<? extends I> ids;

	StandardMultipleTypedLoadCommand(StandardTypedLoadCommand<T> command, Collection<? extends I> ids)
	{
		super(command);
		this.ids = ids;
	}

	@Override
	public Future<Map<I, T>> later()
	{
		throw new UnsupportedOperationException("Not yet implemented");
	}

	@Override
	public Map<I, T> now()
	{
		// the stored type of the id can be defined for the id field
		Field keyField = datastore.idField(command.type);
		String kind = datastore.getConfiguration().typeToKind(command.type);
		Map<I, Key> idsToKeys = new LinkedHashMap<I, Key>(ids.size());
		for (I id : ids)
		{
			idsToKeys.put(id, idToKey(id, keyField, kind));
		}
		final Map<Key, T> keysToInstances = keysToInstances(idsToKeys.values(), propertyRestriction);
		
		// result will have same ordering as ids
		return Maps.transformValues(idsToKeys, new Function<Key, T>()
		{
			@Override
			public T apply(Key key)
			{
				return keysToInstances.get(key);
			}
		});
	}
	
	

}
