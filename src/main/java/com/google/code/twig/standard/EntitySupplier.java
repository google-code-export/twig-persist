package com.google.code.twig.standard;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;

public class EntitySupplier
{
	interface EntitySink
	{
		void pickup();
		Key order();
	}

	private final StrategyObjectDatastore datastore;
	private List<EntitySink> sinks = new ArrayList<EntitySink>();
	private Map<Key, Entity> keysToEntities;
	private final int chunk;
	
	public EntitySupplier(StrategyObjectDatastore datastore, int chunk)
	{
		this.datastore = datastore;
		this.chunk = chunk;
	}
	
	public void register(EntitySink sink)
	{
		sinks.add(sink);
	}
	
	public void demand()
	{
		Set<Key> orders = new HashSet<Key>(chunk);
		int index = 0;
		boolean added = false;
		while (orders.size() < chunk)
		{
			EntitySink sink = sinks.get(index);
			Key order = sink.order();
			if (order != null)
			{
				orders.add(order);
				added = true;
			}
			if (++index == sinks.size())
			{
				index = 0;
				if (added == false)
				{
					break;
				}
				added = false;
			}
		}
		
		keysToEntities = datastore.serviceGet(orders);
		
		for (EntitySink sink : sinks)
		{
			sink.pickup();
		}
	}
	
	public Entity get(Key key)
	{
		return keysToEntities.get(key);
	}
}
