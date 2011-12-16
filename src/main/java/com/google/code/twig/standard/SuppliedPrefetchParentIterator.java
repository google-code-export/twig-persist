package com.google.code.twig.standard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.code.twig.standard.EntitySupplier.EntitySink;
import com.google.common.collect.AbstractIterator;

public class SuppliedPrefetchParentIterator extends AbstractIterator<Entity> implements EntitySink
{
	private final Iterator<Entity> children;
	private final EntitySupplier supplier;
	private List<Key> ordered = new ArrayList<Key>();
	private Iterator<Entity> iterator;

	SuppliedPrefetchParentIterator(
			Iterator<Entity> children,
			EntitySupplier supplier)
	{
		this.children = children;
		this.supplier = supplier;
		supplier.register(this);
	}

	@Override
	public Key order()
	{
		if (children.hasNext())
		{
			Entity child = children.next();
			Key parentKey = child.getKey().getParent();
			ordered.add(parentKey);
			return parentKey;
		}
		else
		{
			return null;
		}
	}

	@Override
	public void pickup()
	{
		List<Entity> parents = new ArrayList<Entity>(ordered.size());
		for (Key parentKey : ordered)
		{
			Entity parent = supplier.get(parentKey);
			parents.add(parent);
		}
		iterator = parents.iterator();
		ordered.clear();
	}

	@Override
	protected Entity computeNext()
	{
		if (iterator == null || !iterator.hasNext())
		{
			if (children.hasNext())
			{
				supplier.demand();
				return computeNext();
			}
			else
			{
				return endOfData();
			}
		}
		else
		{
			return iterator.next();
		}
	}

}
