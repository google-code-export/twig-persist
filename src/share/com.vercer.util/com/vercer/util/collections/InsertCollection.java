package com.vercer.util.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;

public class InsertCollection<T extends Comparable<T>> extends AbstractCollection<T>
{
	private final T item;
	private final Collection<T> delegate;
	
	public InsertCollection(T item, Collection<T> delegate)
	{
		this.item = item;
		this.delegate = delegate;
	}
	
	@Override
	public Iterator<T> iterator()
	{
		return new Iterator<T>()
		{
			Iterator<T> iterator = delegate.iterator();
			boolean used;
			private T next;
			
			@Override
			public boolean hasNext()
			{
				return iterator.hasNext() || !used;
			}

			@Override
			public T next()
			{
				T result = null;
				if (next != null)
				{
					result = next;
					next = null;
				}
				else
				{
					if (iterator.hasNext())
					{
						result = iterator.next();
					}
					
					if (!used)
					{
						if (result == null || item.compareTo(result) > 0)
						{
							next = result; 
							result = item;
							used = true;
						}
					}
				}
				
				return result;
			}

			@Override
			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
	}

	@Override
	public int size()
	{
		return delegate.size() + 1;
	}

}
