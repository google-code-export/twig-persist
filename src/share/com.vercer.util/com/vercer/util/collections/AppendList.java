package com.vercer.util.collections;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

public class AppendList<E> extends AbstractList<E>
{
	private E item;
	private final List<E> list;
	
	public AppendList(List<E> list, E item)
	{
		this.list = list;
		this.item = item;
	}
	
	@Override
	public E get(int index)
	{
		if (index == list.size())
		{
			if (item == null)
			{
				throw new NoSuchElementException();
			}
			return item;
		}
		else
		{
			return list.get(index);
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
				return iterator.hasNext() || returnItem && item != null;
			}

			public E next()
			{
				if (iterator.hasNext())
				{
					return iterator.next();
				}
				else
				{
					if (!returnItem || item == null)
					{
						throw new NoSuchElementException();
					}
					returnItem = false;
					return item;
				}
			}

			public void remove()
			{
				if (!returnItem)
				{
					item = null;
				}
				else
				{
					iterator.remove();
				}
			}
		};
	}

}
