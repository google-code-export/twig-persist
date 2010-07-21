package com.vercer.engine.persist.standard;


public class StandardCommand
{
	final StandardObjectDatastore datastore;

	public StandardCommand(StandardObjectDatastore datastore)
	{
		this.datastore = datastore;
		if (datastore.getTransaction() != null && datastore.getTransaction().isActive() == false)
		{
			datastore.removeTransaction();
		}
	}
}
