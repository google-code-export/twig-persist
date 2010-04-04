package com.vercer.engine.persist.standard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.common.base.Function;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.vercer.engine.persist.ObjectDatastore;

public class PrefetchParentIterator extends AbstractIterator<Entity>
{
	private static final Logger log = Logger.getLogger(PrefetchParentIterator.class.getName());
	private final Iterator<Entity> children;
	private Iterator<Entity> parents;
	private final int chunkSize;
	private final ObjectDatastore datastore;

	PrefetchParentIterator(Iterator<Entity> children, ObjectDatastore datastore, int chunkSize)
	{
		this.children = children;
		this.datastore = datastore;
		this.chunkSize = chunkSize;
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

			long start = System.currentTimeMillis();

			// match the key iterator chunk size
			List<Key> keys = new ArrayList<Key>(chunkSize);
			for (int i = 0; children.hasNext() && i < chunkSize; i++)
			{
				keys.add(children.next().getKey().getParent());
			}

			log.fine("Get child keys" + keys.size() + " keys "
					+ (System.currentTimeMillis() - start));

			start = System.currentTimeMillis();

			// do a bulk get of the keys
			final Map<Key, Entity> map = datastore.getDefaultService().get(keys);

			log.fine("Get parents by key" + keys.size() + " keys "
					+ (System.currentTimeMillis() - start));

			// keep the order of the original keys
			parents = Iterators.transform(keys.iterator(), new Function<Key, Entity>()
			{
				public Entity apply(Key from)
				{
					return map.get(from);
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

}