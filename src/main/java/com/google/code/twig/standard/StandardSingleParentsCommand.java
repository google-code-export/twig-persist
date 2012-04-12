package com.google.code.twig.standard;

import java.util.Iterator;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;

public class StandardSingleParentsCommand<P> extends StandardCommonParentsCommand<P>
{
	private final Iterator<Entity> childEntities;

	StandardSingleParentsCommand(StandardCommonFindCommand<?> command, Iterator<Entity> childEntities, int initialActivationDepth)
	{
		super(command, initialActivationDepth);
		this.childEntities = childEntities;
	}

	@Override
	public Iterator<P> now()
	{
		// no need to cache entities because there are no duplicates
		Iterator<Entity> filtered = childCommand.applyEntityFilter(childEntities);
		Iterator<Entity> parentEntities = new PrefetchParentIterator(filtered, datastore, getFetchSize());
		parentEntities = applyEntityFilter(parentEntities);
		return childCommand.entitiesToInstances(parentEntities, propertyRestriction);
	}

	@Override
	public Future<Iterator<P>> later()
	{
		throw new UnsupportedOperationException("Not yet implemented. Depends on async bulk get.");
	}
}
