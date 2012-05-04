package com.google.code.twig.standard;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LoadCommand;

public class StandardUntypedSingleLoadCommand extends StandardDecodeCommand<StandardUntypedSingleLoadCommand> implements LoadCommand.SingleUntypedLoadCommand
{
	private final Key key;

	StandardUntypedSingleLoadCommand(TranslatorObjectDatastore datastore, Key key, int depth)
	{
		super(datastore, depth);
		this.key = key;
	}
	
	@SuppressWarnings("unchecked")
	public <T> T now()
	{
		return (T) keyToInstance(key, null);
	}
}
