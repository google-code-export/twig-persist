package com.vercer.engine.persist.standard;

import java.util.Iterator;

import com.google.appengine.api.datastore.Entity;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.FindCommand.BaseFindCommand;

abstract class StandardBaseFindCommand<T, C extends BaseFindCommand<C>> implements BaseFindCommand<C>
{
	protected Predicate<Entity> entityPredicate;
	protected Predicate<Property> propertyPredicate;
	protected final StrategyObjectDatastore datastore;

	public StandardBaseFindCommand(StrategyObjectDatastore datastore)
	{
		this.datastore = datastore;
	}

	@SuppressWarnings("unchecked")
	public C filterEntities(Predicate<Entity> filter)
	{
		if (this.entityPredicate != null)
		{
			throw new IllegalStateException("Entity filter was already set");
		}
		this.entityPredicate = filter;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	public C filterProperties(Predicate<Property> filter)
	{
		if (this.propertyPredicate != null)
		{
			throw new IllegalStateException("Property filter was already set");
		}
		this.propertyPredicate = filter;
		return (C) this;
	}

	protected Iterator<Entity> applyEntityFilter(Iterator<Entity> entities)
	{
		if (this.entityPredicate != null)
		{
			entities = Iterators.filter(entities, this.entityPredicate);
		}
		return entities;
	}
}
