package com.vercer.engine.persist.standard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query.SortPredicate;

public class StandardMergeParentsCommand<P> extends StandardBaseParentsCommand<P>
{
	private final List<Iterator<Entity>> childEntityIterators;
	private final List<SortPredicate> sorts;

	public StandardMergeParentsCommand(StandardTypedFindCommand<?, ?> command, List<Iterator<Entity>> childEntityIterators, List<SortPredicate> sorts)
	{
		super(command);
		this.childEntityIterators = childEntityIterators;
		this.sorts = sorts;
	}

	public Iterator<P> returnParentsNow()
	{
		// keys only child queries cannot be sorted as fields are missing
		if (childCommand.getRootCommand().isKeysOnly())
		{
			// make an entity cache with room to hold a round of fetches
			final int maxSize = getFetchSize() * childEntityIterators.size() ;
			LinkedHashMap<Key, Entity> keyToEntity = new LinkedHashMap<Key, Entity>((int) (maxSize / 0.75))
			{
				private static final long serialVersionUID = 1L;

				@Override
				protected boolean removeEldestEntry(java.util.Map.Entry<Key, Entity> eldest)
				{
					return size() >= maxSize;
				}
			};
			
			// cannot merge children so must get parent entities first
			List<Iterator<Entity>> parentEntityIterators = new ArrayList<Iterator<Entity>>(childEntityIterators.size());
			for (Iterator<Entity> child : childEntityIterators)
			{
				// convert children to parents - may be dups so use a cache
				Iterator<Entity> parentEntities = childEntitiesToParentEntities(child, keyToEntity);
				parentEntities = applyEntityFilter(parentEntities);
				parentEntityIterators.add(parentEntities);
			}
			
			// merge all the parent iterators into a single iterator
			Iterator<Entity> mergedParentEntities = mergeEntities(parentEntityIterators, sorts);
			
			// convert the entities into instances to return
			return entityToInstanceIterator(mergedParentEntities, false);
		}
		else
		{
			// we can merge the children first which gets rid of duplicates
			Iterator<Entity> mergedChildEntities = mergeEntities(childEntityIterators, sorts);
			mergedChildEntities = applyEntityFilter(mergedChildEntities);
			
			// get parents for all children at the same time - no dups so no cache
			Iterator<Entity> parentEntities = childEntitiesToParentEntities(mergedChildEntities, null);
			return entityToInstanceIterator(parentEntities, false);
		}
	}		
}
