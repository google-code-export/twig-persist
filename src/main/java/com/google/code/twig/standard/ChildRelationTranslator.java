/**
 *
 */
package com.google.code.twig.standard;

import com.google.appengine.api.datastore.Key;


class ChildRelationTranslator extends RelationTranslator
{
	ChildRelationTranslator(TranslatorObjectDatastore strategyObjectDatastore)
	{
		super(strategyObjectDatastore);
	}

	@Override
	protected Key getParentKey()
	{
		return datastore.encodeKeySpec.toKey();
	}
}