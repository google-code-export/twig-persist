package com.vercer.engine.persist.standard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query.SortPredicate;

public class StandardMergeParentsCommand<P> extends StandardBaseParentsCommand<P>
{
	private final List<Iterator<Entity>> childIterators;
	private final List<SortPredicate> sorts;

	public StandardMergeParentsCommand(StandardTypedFindCommand<?, ?> command, List<Iterator<Entity>> childEntityIterators, List<SortPredicate> sorts)
	{
		super(command);
		this.childIterators = childEntityIterators;
		this.sorts = sorts;
	}

	public Iterator<P> returnParentsNow()
	{
		List<Iterator<Entity>> parentEntityIterators = new ArrayList<Iterator<Entity>>(childIterators.size());
		for (Iterator<Entity> child : childIterators)
		{
			Iterator<Entity> parentEntities = command.childEntitiesToParentEntities(child);
			parentEntities = applyEntityFilter(parentEntities);
			parentEntityIterators.add(parentEntities);
		}
		Iterator<Entity> mergedParentEntities = command.mergeEntities(parentEntityIterators, sorts);

		return command.entityToInstanceIterator(mergedParentEntities, false);
	}

}
