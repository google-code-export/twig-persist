package com.vercer.engine.persist.standard;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.vercer.util.reference.ObjectReference;
import com.vercer.util.reference.ReadOnlyObjectReference;
import com.vercer.util.reference.SimpleObjectReference;

public class KeySpecification
{
	private String kind;
	private ObjectReference<Key> parentKeyReference;
	private String name;
	private Long id;

	public KeySpecification()
	{
	}

	public KeySpecification(String kind, Key parentKey, String name)
	{
		this.kind = kind;
		this.name = name;
		this.parentKeyReference = parentKey == null ? null : new SimpleObjectReference<Key>(parentKey);
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public Long getId()
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
		return kind != null && (name != null || id != null);
	}

	public Key toKey()
	{
		if (isComplete())
		{
			if (parentKeyReference == null)
			{
				if (id == null)
				{
					return KeyFactory.createKey(kind, name);
				}
				else
				{
					return KeyFactory.createKey(kind, id);
				}
			}
			else
			{
				if (id == null)
				{
					return KeyFactory.createKey(parentKeyReference.get(), kind, name);
				}
				else
				{
					return KeyFactory.createKey(parentKeyReference.get(), kind, id);
				}
			}
		}
		else
		{
			throw new IllegalStateException("Key specification is incomplete. "
					+ " You may need to define a key name for this or its parent instance");
		}
	}

	public ObjectReference<Key> toObjectReference()
	{
		return new ReadOnlyObjectReference<Key>()
		{
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

		if (name == null)
		{
			name = specification.name;
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

	public void setId(Long id)
	{
		this.id = id;
	}
}
