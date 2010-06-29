package com.vercer.engine.persist.standard;

import com.google.appengine.api.datastore.DatastoreService;
import com.vercer.engine.persist.strategy.CombinedStrategy;

public class StandardObjectDatastore extends StrategyObjectDatastore
{
	public StandardObjectDatastore(DatastoreService service, CombinedStrategy strategy)
	{
		super(service, strategy);
	}
	
	
}
