/**
 *
 */
package com.vercer.engine.persist.standard;

import com.google.appengine.api.datastore.Key;


class ChildEntityTranslator extends EntityTranslator
{
	ChildEntityTranslator(StrategyObjectDatastore strategyObjectDatastore)
	{
		super(strategyObjectDatastore);
	}
	
	@Override
	protected Key getParentKey()
	{
		return datastore.encodeKeySpec.toKey();
	}
}