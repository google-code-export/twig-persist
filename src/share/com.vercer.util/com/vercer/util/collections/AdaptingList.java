package com.vercer.util.collections;

import java.util.AbstractList;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

public abstract class AdaptingList<Target, Source> extends AbstractList<Target>
{
	private final List<Source> source;

	public AdaptingList(List<Source> source)
	{
		this.source = source;
	}

	protected abstract Target wrap(Source source);
	
	protected abstract Source unwrap(Target target);
	
	@Override
	public Target get(int index)
	{
		return wrap(this.source.get(index));
	}
	
	@Override
	public Target set(int index, Target element)
	{
		return wrap(this.source.set(index, unwrap(element)));
	}

	@Override
	public boolean add(Target o)
	{
		return this.source.add(unwrap(o));
	}
	
	@Override
	public void add(int i, Target o)
	{
		this.source.add(i, unwrap(o));
	}
	
	@Override
	public boolean remove(Object o)
	{
		return this.source.remove(o);
	}
	
	@Override
	public int size()
	{
		return this.source.size();
	}
	
	@Override
	public Iterator<Target> iterator()
	{
		final Iterator<Source> iterator = this.source.iterator();
		return new Iterator<Target>()
		{
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			public Target next()
			{
				return wrap(iterator.next());
			}

			public void remove()
			{
				iterator.remove();
			}
		};
	}
	
	@Override
	public ListIterator<Target> listIterator(int index)
	{
		final ListIterator<Source> iterator = this.source.listIterator(index);
		return new ListIterator<Target>()
		{
			public void add(Target o)
			{
				iterator.add(unwrap(o));
			}

			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			public boolean hasPrevious()
			{
				return iterator.hasPrevious();
			}

			public Target next()
			{
				return wrap(iterator.next());
			}

			public int nextIndex()
			{
				return iterator.nextIndex();
			}

			public Target previous()
			{
				return wrap(iterator.previous());
			}

			public int previousIndex()
			{
				return iterator.previousIndex();
			}

			public void remove()
			{
				iterator.remove();
			}

			public void set(Target o)
			{
				iterator.set(unwrap(o));
			}
		};
	}
	
	
}
