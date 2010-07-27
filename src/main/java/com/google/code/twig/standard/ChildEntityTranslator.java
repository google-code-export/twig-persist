/**
 *
 */
package com.google.code.twig.standard;

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