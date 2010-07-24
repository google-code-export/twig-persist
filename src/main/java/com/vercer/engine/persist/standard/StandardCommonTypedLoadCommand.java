package com.vercer.engine.persist.standard;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.Restriction;
import com.vercer.engine.persist.LoadCommand.CommonTypedLoadCommand;

class StandardCommonTypedLoadCommand<T, C extends StandardCommonTypedLoadCommand<T, C>> extends StandardDecodeCommand implements CommonTypedLoadCommand<T, C>
{
	Restriction<Entity> entityRestriction;
	Restriction<Property> propertyRestriction;
	Key parentKey;

	StandardCommonTypedLoadCommand(StrategyObjectDatastore datastore)
	{
		super(datastore);
	}

	@SuppressWarnings("unchecked")
	@Override
	public C restrictEntities(Restriction<Entity> restriction)
	{
		this.entityRestriction = restriction;
		return (C) this;
	}

	@SuppressWarnings("unchecked")
	@Override
	public C restrictProperties(Restriction<Property> restriction)
	{
		this.propertyRestriction = restriction;
		return (C) this;
	}
	
	@SuppressWarnings("unchecked")
	public final C parent(Object parent)
	{
		parentKey = datastore.associatedKey(parent);
		if (parentKey == null)
		{
			throw new IllegalArgumentException("Parent is not associated: " + parent);
		}
		return (C) this;
	}
}
