package com.google.code.twig.standard;

import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.AsyncDatastoreHelper;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.code.twig.FindCommand.CommonFindCommand;
import com.google.code.twig.Property;
import com.google.code.twig.Restriction;
import com.google.code.twig.util.RestrictionToPredicateAdaptor;
import com.google.code.twig.util.SortedMergeIterator;
import com.google.common.base.Function;
import com.google.common.collect.Iterators;

/**
 * Contains functionality common to all both TypedFindCommand and ParentsCommand
 * 
 * @author John Patterson <john@vercer.com>
 *
 * @param <T> The type of the instance that will be returned
 * @param <C> The concrete type that is returned from chained methods  
 */
abstract class StandardCommonFindCommand<T, C extends CommonFindCommand<C>> extends StandardDecodeCommand implements CommonFindCommand<C>
{
	Restriction<Entity> entityPredicate;
	Restriction<Property> propertyPredicate;

	StandardCommonFindCommand(StrategyObjectDatastore datastore)
	{
		super(datastore);
	}

	@SuppressWarnings("unchecked")
	@Override
	public C restrictEntities(Restriction<Entity> filter)
	{
		if (this.entityPredicate != null)
		{
			throw new IllegalStateException("Entity filter was already set");
		}
		this.entityPredicate = filter;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public C restrictProperties(Restriction<Property> filter)
	{
		if (this.propertyPredicate != null)
		{
			throw new IllegalStateException("Property filter was already set");
		}
		this.propertyPredicate = filter;
		return (C) this;
	}

	Iterator<Entity> applyEntityFilter(Iterator<Entity> entities)
	{
		if (this.entityPredicate != null)
		{
			entities = Iterators.filter(entities, new RestrictionToPredicateAdaptor<Entity>(entityPredicate));
		}
		return entities;
	}

	Iterator<Entity> mergeEntities(List<Iterator<Entity>> iterators, List<SortPredicate> sorts)
	{
		Iterator<Entity> merged;
		if (sorts != null && !sorts.isEmpty())
		{
			Comparator<Entity> comparator = AsyncDatastoreHelper.newEntityComparator(sorts);
			merged = new SortedMergeIterator<Entity>(comparator, iterators);
		}
		else
		{
			merged = Iterators.concat(iterators.iterator());
		}
		return merged;
	}
	
	<R> Iterator<R> entityToInstanceIterator(Iterator<Entity> entities, boolean keysOnly)
	{
		Function<Entity, R> function = new EntityToInstanceFunction<R>(propertyPredicate);
		return Iterators.transform(entities, function);
	}
	
	private final class EntityToInstanceFunction<R> implements Function<Entity, R>
	{
		private final Restriction<Property> predicate;

		public EntityToInstanceFunction(Restriction<Property> propertyPredicate)
		{
			this.predicate = propertyPredicate;
		}

		@SuppressWarnings("unchecked")
		@Override
		public R apply(Entity entity)
		{
			return (R) entityToInstance(entity, predicate);
		}
	}
}
