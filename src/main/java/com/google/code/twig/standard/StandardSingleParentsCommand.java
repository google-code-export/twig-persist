package com.vercer.engine.persist.standard;

import java.util.Iterator;

import com.google.appengine.api.datastore.Entity;

class StandardSingleParentsCommand<P> extends StandardCommonParentsCommand<P>
{
	private final Iterator<Entity> childEntities;

	StandardSingleParentsCommand(StandardTypedFindCommand<?, ?> command, Iterator<Entity> childEntities)
	{
		super(command);
		this.childEntities = childEntities;
	}

	public Iterator<P> returnParentsNow()
	{
		// no need to cache entities because there are no duplicates
		Iterator<Entity> filtered = applyEntityFilter(childEntities);
		Iterator<Entity> parentEntities = new PrefetchParentIterator(filtered, datastore, getFetchSize());
		parentEntities = applyEntityFilter(parentEntities);
		return childCommand.entityToInstanceIterator(parentEntities, false);
	}
}
