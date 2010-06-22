package com.vercer.engine.persist.standard;


public class StandardCommand
{
	final StrategyObjectDatastore datastore;

	public StandardCommand(StrategyObjectDatastore datastore)
	{
		this.datastore = datastore;
		if (datastore.getTransaction() != null && datastore.getTransaction().isActive() == false)
		{
			datastore.removeTransaction();
		}
	}
}
