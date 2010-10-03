package com.vercer.util.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class PrependSet<E> extends AbstractSet<E>
{
	private final E item;
	private final Set<E> set;
	
	public PrependSet(E item, Set<E> list)
	{
		this.item = item;
		this.set = list;
	}
	
	@Override
	public int size()
	{
		return set.size() + (item == null ? 0 : 1);
	}
	
	@Override
	public Iterator<E> iterator()
	{
		return new Iterator<E>()
		{
			boolean returnItem = true;
			Iterator<E> iterator = set.iterator();
			
			public boolean hasNext()
			{
				return returnItem && item != null || iterator.hasNext();
			}

			public E next()
			{
				if (returnItem)
				{
					returnItem = false;
					return item;
				}
				else
				{
					return iterator.next();
				}
			}

			public void remove()
			{
				throw new UnsupportedOperationException(); 
			}
		};
	}
	
	public E getItem()
	{
		return item;
	}
	
	public Set<E> getSet()
	{
		return set;
	}

}
