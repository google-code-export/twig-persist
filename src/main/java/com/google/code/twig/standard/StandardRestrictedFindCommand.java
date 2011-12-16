package com.google.code.twig.standard;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.AsyncDatastoreHelper;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.code.twig.FindCommand.RestrictedFindCommand;
import com.google.code.twig.Property;
import com.google.code.twig.Restriction;
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
abstract class StandardRestrictedFindCommand<C extends RestrictedFindCommand<C>> extends StandardDecodeCommand implements RestrictedFindCommand<C>
{
	Restriction<Entity> entityRestriction;
	Restriction<Property> propertyRestriction;

	StandardRestrictedFindCommand(TranslatorObjectDatastore datastore)
	{
		super(datastore);
	}

	@SuppressWarnings("unchecked")
	@Override
	public C restrictEntities(Restriction<Entity> filter)
	{
		if (this.entityRestriction != null)
		{
			throw new IllegalStateException("Entity filter was already set");
		}
		this.entityRestriction = filter;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public C restrictProperties(Restriction<Property> filter)
	{
		if (this.propertyRestriction != null)
		{
			throw new IllegalStateException("Property filter was already set");
		}
		this.propertyRestriction = filter;
		return (C) this;
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
		Comparator<Entity> comparator = AsyncDatastoreHelper.newEntityComparator(sorts);
		merged = new SortedMergeIterator<Entity>(comparator, iterators);
		return merged;
	}

}
