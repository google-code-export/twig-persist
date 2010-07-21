package com.vercer.engine.persist.standard;

import com.vercer.engine.persist.FindCommand;
import com.vercer.engine.persist.StoreCommand;
import com.vercer.engine.persist.strategy.ActivationStrategy;
import com.vercer.engine.persist.strategy.CombinedStrategy;
import com.vercer.engine.persist.strategy.FieldStrategy;
import com.vercer.engine.persist.strategy.RelationshipStrategy;
import com.vercer.engine.persist.strategy.StorageStrategy;

public class StandardObjectDatastore extends StrategyObjectDatastore
{
	public StandardObjectDatastore(CombinedStrategy strategy)
	{
		super(strategy);
	}
	
	public StandardObjectDatastore(
			RelationshipStrategy relationshipStrategy,
			StorageStrategy storageStrategy, 
			ActivationStrategy activationStrategy,
			FieldStrategy fieldStrategy)
	{
		super(relationshipStrategy, storageStrategy, activationStrategy, fieldStrategy);
	}

	@Override
	public final FindCommand find()
	{
		return new StandardFindCommand(this);
	}

	@Override
	public final StoreCommand store()
	{
		return new StandardStoreCommand(this);
	}
}
