package com.google.code.twig.standard;

class StandardCommand
{
	protected final TranslatorObjectDatastore datastore;

	StandardCommand(TranslatorObjectDatastore datastore)
	{
		// check we have the same thread
		if (datastore.thread != Thread.currentThread())
		{
			throw new IllegalStateException("Dectected use by more than one thread");
		}
		
		this.datastore = datastore;
		if (datastore.getTransaction() != null && datastore.getTransaction().isActive() == false)
		{
			datastore.removeTransaction();
		}
	}
}
