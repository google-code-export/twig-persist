package com.google.code.twig.standard;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityComparatorAccessor;
import com.google.appengine.api.datastore.EntityProtoComparators.EntityProtoComparator;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.apphosting.api.DatastorePb.Query.Order;
import com.google.code.twig.util.RestrictionToPredicateAdaptor;
import com.google.code.twig.util.SortedMergeIterator;
import com.google.common.collect.Iterators;

/**
 * Contains functionality common to all both TypedFindCommand and ParentsCommand
 * 
 * @author John Patterson <john@vercer.com>
 *
 * @param <T> The type of the instance that will be returned
 * @param <C> The concrete type that is returned from chained methods  
 */
abstract class StandardRestrictedFindCommand<C extends StandardRestrictedFindCommand<C>> extends StandardDecodeCommand<C>
{
	StandardRestrictedFindCommand(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}

	Iterator<Entity> applyEntityFilter(Iterator<Entity> entities)
	{
		if (this.entityRestriction != null)
		{
			entities = Iterators.filter(entities, new RestrictionToPredicateAdaptor<Entity>(entityRestriction));
		}
		return entities;
	}

	Iterator<Entity> mergeEntities(List<Iterator<Entity>> iterators, List<SortPredicate> sorts)
	{
		Iterator<Entity> merged;
		Comparator<Entity> comparator = EntityComparatorAccessor.newEntityComparator(sorts);
		merged = new SortedMergeIterator<Entity>(comparator, iterators);
		return merged;
	}
}
