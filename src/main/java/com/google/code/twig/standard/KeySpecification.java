package com.google.code.twig.standard;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.code.twig.standard.InstanceKeyCache.KeyReference;
import com.google.code.twig.util.reference.ObjectReference;
import com.google.code.twig.util.reference.SimpleObjectReference;

public class KeySpecification
{
	private String kind;
	private ObjectReference<Key> parentKeyReference;
	private Object id;

	public KeySpecification()
	{
	}

	public KeySpecification(String kind, Key parentKey, Object id)
	{
		if (!(id == null || id instanceof String || id instanceof Long))
		{
			throw new IllegalArgumentException("Id must be a long or String but was " + id);
		}
		
		this.kind = kind;
		this.id = id;
		this.parentKeyReference = parentKey == null ? null : new SimpleObjectReference<Key>(parentKey);
	}

	public Object getId()
	{
		return id;
	}

	public String getKind()
	{
		return kind;
	}

	public void setKind(String kind)
	{
		this.kind = kind;
	}

	public void setParentKeyReference(ObjectReference<Key> parentKeyReference)
	{
		this.parentKeyReference = parentKeyReference;
	}

	public ObjectReference<Key> getParentKeyReference()
	{
		return parentKeyReference;
	}

	public boolean isComplete()
	{
		return kind != null && id != null;
	}

	public Key toKey()
	{
		if (isComplete())
		{
			if (parentKeyReference == null)
			{
				if (id instanceof String)
				{
					return KeyFactory.createKey(kind, (String) id);
				}
				else
				{
					return KeyFactory.createKey(kind, (Long) id);
				}
			}
			else
			{
				if (id instanceof String)
				{
					return KeyFactory.createKey(parentKeyReference.get(), kind, (String) id);
				}
				else
				{
					return KeyFactory.createKey(parentKeyReference.get(), kind, (Long) id);
				}
			}
		}
		else
		{
			throw new IllegalStateException("Key specification is incomplete. "
					+ " You may need to define an id for instance with kind " + kind);
		}
	}

	public KeyReference toKeyReference()
	{
		return new KeyReference(null)
		{
			private static final long serialVersionUID = 1L;

			@Override
			public Key get()
			{
				return toKey();
			}
		};
	}

	public void merge(KeySpecification specification)
	{
		// fill in any blanks with info we have gathered from the instance
		// fields
		if (parentKeyReference == null)
		{
			parentKeyReference = specification.parentKeyReference;
		}

		if (id == null)
		{
			id = specification.id;
		}

		if (kind == null)
		{
			kind = specification.kind;
		}
	}

	public void setId(long id)
	{
		this.id = id;
	}
	
	public void setName(String name)
	{
		this.id = name;
	}
}
