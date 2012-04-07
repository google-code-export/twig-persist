package com.google.code.twig.util;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

public final class Entities
{
	public static Entity changeKind(Entity entity, String kind)
	{
		Key key;
		if (entity.getKey().getName() == null)
		{
			if (entity.getParent() == null)
			{
				key = KeyFactory.createKey(kind, entity.getKey().getId());
			}
			else
			{
				key = KeyFactory.createKey(entity.getParent(), kind, entity.getKey().getId());
			}
		}
		else
		{
			if (entity.getParent() == null)
			{
				key = KeyFactory.createKey(kind, entity.getKey().getName());
			}
			else
			{
				key = KeyFactory.createKey(entity.getParent(), kind, entity.getKey().getName());
			}
		}
		Entity changed = new Entity(key);
		changed.setPropertiesFrom(entity);
		return changed;
	}

	public static Entity createEntity(String kind, String name, Key parent)
	{
		if (parent == null)
		{
			if (name == null)
			{
				return new Entity(kind);
			}
			else
			{
				return new Entity(kind, name);
			}
		}
		else
		{
			if (name == null)
			{
				return new Entity(kind, parent);
			}
			else
			{
				return new Entity(kind, name, parent);
			}
		}
	}
	
	public static Entity delta(Entity from, Entity to)
	{
		// TODO
		return null;
	}
}
