package com.google.code.twig.standard;

import java.util.Iterator;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.FetchOptions;
import com.google.code.twig.FindCommand.ParentsCommand;

abstract class StandardCommonParentsCommand<P> extends
		StandardRestrictedFindCommand<StandardCommonParentsCommand<P>> implements ParentsCommand<P>
{
	protected final StandardCommonFindCommand<?> childCommand;

	StandardCommonParentsCommand(StandardCommonFindCommand<?> command, int initialActivationDepth)
	{
		super(command.datastore, initialActivationDepth);
		this.childCommand = command;
	}

	public Future<Iterator<P>> returnParentsLater()
	{
		// TODO depends on async get being implemented
		throw new UnsupportedOperationException("Not implemented yet - depends on async get");
	}

	protected int getFetchSize()
	{
		@SuppressWarnings("deprecation")
		int fetch = FetchOptions.DEFAULT_CHUNK_SIZE;
		FetchOptions fetchOptions = childCommand.getRootCommand().getFetchOptions();
		if (fetchOptions != null && fetchOptions.getChunkSize() != null)
		{
			if (fetchOptions.getChunkSize() != fetchOptions.getPrefetchSize())
			{
				throw new IllegalArgumentException(
						"Must have same fetchFirst and fetchNextBy to get parents");
			}
			fetch = fetchOptions.getChunkSize();
		}
		return fetch;
	}
}