package com.vercer.engine.persist.standard;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.FindCommand.ParentsCommand;

public abstract class StandardBaseParentsCommand<P> extends StandardBaseFindCommand<P, ParentsCommand<P>> implements ParentsCommand<P>
{
	protected final StandardTypedFindCommand<?, ?> childCommand;

	public StandardBaseParentsCommand(StandardTypedFindCommand<?, ?> command)
	{
		super(command.datastore);
		this.childCommand = command;
	}

	public Future<Iterator<P>> returnParentsLater()
	{
		// TODO depends on async get being implemented
		throw new UnsupportedOperationException("Not implemented yet - depends on async get");
	}
	
	/**
	 * @param childEntities
	 * @param source Allows a cache to be used to 
	 * @return
	 */
	Iterator<Entity> childEntitiesToParentEntities(Iterator<Entity> childEntities, final Map<Key, Entity> cache)
	{
		childEntities = childCommand.applyEntityFilter(childEntities);
		return new PrefetchParentIterator(childEntities, datastore, getFetchSize())
		{
			@Override
			protected Map<Key, Entity> keysToEntities(List<Key> keys)
			{
				if (cache == null)
				{
					return super.keysToEntities(keys);
				}
				else
				{
					Map<Key, Entity> result = new HashMap<Key, Entity>(keys.size());
					List<Key> missing = null;
					for (Key key : keys)
					{
						Entity entity = cache.get(key);
						if (entity != null)
						{
							result.put(key, entity);
						}
						else
						{
							if (missing == null)
							{
								missing = new ArrayList<Key>(keys.size());
							}
							missing.add(key);
						}
					}
					
					if (missing != null)
					{
						Map<Key, Entity> entities = super.keysToEntities(missing);
						Set<Entry<Key,Entity>> entrySet = entities.entrySet();
						for (Entry<Key, Entity> entry : entrySet)
						{
							cache.put(entry.getKey(), entry.getValue());
						}
						result.putAll(entities);
					}
					return result;
				}
			}
		};
	}

	protected int getFetchSize()
	{
		@SuppressWarnings("deprecation")
		int fetch = FetchOptions.DEFAULT_CHUNK_SIZE;
		FetchOptions fetchOptions = childCommand.getRootCommand().getFetchOptions();
		if (fetchOptions != null)
		{
			if (fetchOptions.getChunkSize() != fetchOptions.getPrefetchSize())
			{
				throw new IllegalArgumentException("Must have same fetchFirst and fetchNextBy to get parents");
			}
			fetch = fetchOptions.getChunkSize();
		}
		return fetch;
	}
}