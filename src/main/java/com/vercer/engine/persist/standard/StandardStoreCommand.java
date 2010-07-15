package com.vercer.engine.persist.standard;

import java.util.Arrays;
import java.util.Collection;

import com.vercer.engine.persist.StoreCommand;

public class StandardStoreCommand extends StandardCommand implements StoreCommand
{
	public StandardStoreCommand(StrategyObjectDatastore datastore)
	{
		super(datastore);
	}

	public <T> SingleStoreCommand<T> instance(T instance)
	{
		return new StandardSingleStoreCommand<T>(this, instance);
	}

	public <T> MultipleStoreCommand<T> instances(Collection<T> instances)
	{
		return new StandardMultipleStoreCommand<T>(this, instances);
	}
	
	public <T> MultipleStoreCommand<T> instances(T... instances) 
	{
		return new StandardMultipleStoreCommand<T>(this, Arrays.asList(instances));
	};
}
