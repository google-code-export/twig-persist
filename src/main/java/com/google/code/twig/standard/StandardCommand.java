package com.google.code.twig.standard;

class StandardCommand
{
	final StrategyObjectDatastore datastore;

	StandardCommand(StrategyObjectDatastore datastore)
	{
		this.datastore = datastore;
		if (datastore.getTransaction() != null && datastore.getTransaction().isActive() == false)
		{
			datastore.removeTransaction();
		}
	}
}
