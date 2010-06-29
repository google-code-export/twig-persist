package com.vercer.engine.persist.standard;

import java.util.Iterator;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
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
		throw new UnsupportedOperationException("Not implemented yet");
	}
	
	/**
	 * @param childEntities
	 * @param source Allows a cache to be used to 
	 * @return
	 */
	Iterator<Entity> childEntitiesToParentEntities(Iterator<Entity> childEntities)
	{
		childEntities = childCommand.applyEntityFilter(childEntities);
		@SuppressWarnings("deprecation")
		int fetch = FetchOptions.DEFAULT_CHUNK_SIZE;
		FetchOptions fetchOptions = childCommand.getFetchOptions();
		if (fetchOptions != null)
		{
			if (fetchOptions.getChunkSize() != fetchOptions.getPrefetchSize())
			{
				throw new IllegalArgumentException("Must have same fetchFirst and fetchNextBy to get parents");
			}
			fetch = fetchOptions.getChunkSize();
		}
		
		Iterator<Entity> parentEntities = new PrefetchParentIterator(childEntities, datastore, fetch);
		return parentEntities;
	}
}