package com.vercer.util.collections;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class MergeList<T> extends AbstractList<T>
{
	private final List<List<T>> lists;
	
	public MergeList()
	{
		lists = new  ArrayList<List<T>>();
	}
	
	public MergeList(List<T>... lists)
	{
		this.lists = Arrays.asList(lists);
	}
	
	public MergeList(List<List<T>> lists)
	{
		this.lists = lists;
	}

	public MergeList(int size)
	{
		lists = new  ArrayList<List<T>>(size);
	}

	@Override
	public T get(int index)
	{
		int total = 0;
		List<T> last = null;
		for (List<T> list : lists)
		{
			if (total + list.size() > index)
			{
				break;
			}
			else
			{
				total += list.size();
				last = list;
			}
		}

		return last.get(index - total);
	}

	@Override
	public int size()
	{
		int size = 0;
		for (List<T> list : lists)
		{
			size += list.size();
		}
		return size;
	}
	
	@Override
	public Iterator<T> iterator()
	{
		return new Iterator<T>()
		{
			private Iterator<List<T>> listItr = lists.iterator();
			private Iterator<T> currentItr;
			
			public boolean hasNext()
			{
				// ensure current iterator is fresh
				if (currentItr == null || currentItr.hasNext() == false)
				{
					if (listItr.hasNext() == false)
					{
						return false;
					}
					currentItr = listItr.next().iterator();
				}
				
				return currentItr.hasNext();
			}

			public T next()
			{
				return currentItr.next();
			}

			public void remove()
			{
				currentItr.remove();
			}
			
		};
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean addAll(Collection<? extends T> c)
	{
		if (c instanceof List<?>)
		{
			lists.add((List<T>) c);
		}
		throw new IllegalArgumentException();
	}
}
