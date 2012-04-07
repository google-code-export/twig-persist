package com.google.code.twig.standard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;

class PrefetchParentIterator extends AbstractIterator<Entity>
{
	private final Iterator<Entity> children;
	private Iterator<Entity> parents;
	private final int fetchBy;
	private final TranslatorObjectDatastore datastore;

	PrefetchParentIterator(Iterator<Entity> children, TranslatorObjectDatastore datastore, int fetchBy)
	{
		this.children = children;
		this.datastore = datastore;
		this.fetchBy = fetchBy;
	}

	@Override
	protected Entity computeNext()
	{
		if (parents == null)
		{
			if (!children.hasNext())
			{
				return endOfData();
			}

			// match the key iterator chunk size
			List<Key> keys = new ArrayList<Key>(fetchBy);
			for (int i = 0; i < fetchBy && children.hasNext(); i++)
			{
				keys.add(children.next().getKey().getParent());
			}

			// do a bulk get of the keys
			final Map<Key, Entity> keyToEntity = keysToEntities(keys);

			// keep the order of the original keys
			parents = Iterators.transform(keys.iterator(), new Function<Key, Entity>()
			{
				public Entity apply(Key from)
				{
					return keyToEntity.get(from);
				}
			});

			if (parents.hasNext() == false)
			{
				return endOfData();
			}
		}

		if (parents.hasNext())
		{
			return parents.next();
		}
		else
		{
			parents = null;
			return computeNext();
		}
	}

	protected Map<Key, Entity> keysToEntities(List<Key> keys)
	{
		return datastore.serviceGet(keys, null);
	}
}