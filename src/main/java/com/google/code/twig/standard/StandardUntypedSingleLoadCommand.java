package com.google.code.twig.standard;

import com.google.appengine.api.datastore.Key;

public class StandardUntypedSingleLoadCommand extends StandardDecodeCommand<StandardUntypedSingleLoadCommand>
{
	private final Key key;

	StandardUntypedSingleLoadCommand(TranslatorObjectDatastore datastore, Key key)
	{
		super(datastore);
		this.key = key;
	}
	
	public <T> T now()
	{
		@SuppressWarnings("unchecked")
		T result = (T) keyToInstance(key, null);
		return result;
	}
}
