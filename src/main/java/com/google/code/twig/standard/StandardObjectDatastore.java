package com.google.code.twig.standard;

import com.google.code.twig.strategy.ActivationStrategy;
import com.google.code.twig.strategy.CombinedStrategy;
import com.google.code.twig.strategy.FieldStrategy;
import com.google.code.twig.strategy.RelationshipStrategy;
import com.google.code.twig.strategy.StorageStrategy;

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
}
