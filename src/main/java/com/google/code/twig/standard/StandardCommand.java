package com.google.code.twig.standard;

class StandardCommand
{
	final TranslatorObjectDatastore datastore;

	StandardCommand(TranslatorObjectDatastore datastore)
	{
		this.datastore = datastore;
		if (datastore.getTransaction() != null && datastore.getTransaction().isActive() == false)
		{
			datastore.removeTransaction();
		}
	}
}
