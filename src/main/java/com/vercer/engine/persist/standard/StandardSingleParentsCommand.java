package com.vercer.engine.persist.standard;

import java.util.Iterator;

import com.google.appengine.api.datastore.Entity;

public class StandardSingleParentsCommand<P> extends StandardBaseParentsCommand<P>
{
	private final Iterator<Entity> childEntities;

	public StandardSingleParentsCommand(StandardTypedFindCommand<?, ?> command, Iterator<Entity> childEntities)
	{
		super(command);
		this.childEntities = childEntities;
	}

	public Iterator<P> returnParentsNow()
	{
		// no need to cache entities because there are no duplicates
		Iterator<Entity> parentEntities = childEntitiesToParentEntities(childEntities, null);
		parentEntities = applyEntityFilter(parentEntities);
		return childCommand.entityToInstanceIterator(parentEntities, false);
	}
}
