package com.vercer.util.collections;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;

public class PrependList<E> extends AbstractList<E>
{
	private final E item;
	private final List<E> list;
	
	public PrependList(E item, List<E> list)
	{
		this.item = item;
		this.list = list;
	}
	
	@Override
	public E get(int index)
	{
		if (index == 0)
		{
			return item;
		}
		else
		{
			return list.get(index - 1);
		}
	}
	
	@Override
	public int size()
	{
		return list.size() + (item == null ? 0 : 1);
	}
	
	@Override
	public Iterator<E> iterator()
	{
		return new Iterator<E>()
		{
			boolean returnItem = true;
			Iterator<E> iterator = list.iterator();
			
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

}
