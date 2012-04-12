package com.vercer.util.collections;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;

public class AppendCollection<T> extends AbstractCollection<T>
{
	private final Collection<T> delegate;
	private T item;

	public AppendCollection(Collection<T> delegate, T item)
	{
		this.delegate = delegate;
		this.item = item;
	}

	@Override
	public Iterator<T> iterator()
	{
		return new Iterator<T>()
		{
			boolean returnItem = true;
			Iterator<T> iterator = delegate.iterator();

			public boolean hasNext()
			{
				return iterator.hasNext() || returnItem && item != null;
			}

			public T next()
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

	@Override
	public int size()
	{
		return delegate.size() + 1;
	}

	public T getItem()
	{
		return this.item;
	}

	public Collection<T> getDelegate()
	{
		return this.delegate;
	}
}
