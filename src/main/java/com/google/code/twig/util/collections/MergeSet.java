package com.google.code.twig.util.collections;

import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.Iterators;

public class MergeSet<T> extends AbstractSet<T>
{
	private int size;
	private final List<Collection<? extends T>> sets;

	public MergeSet()
	{
		this.sets = new ArrayList<Collection<? extends T>>();
	}

	public MergeSet(int size)
	{
		this.sets = new ArrayList<Collection<? extends T>>(size);
	}

	@Override
	public Iterator<T> iterator()
	{
		Iterator<Iterator<? extends T>> iterators = new Iterator<Iterator<? extends T>>()
		{
			Iterator<Collection<? extends T>> iterator = sets.iterator();
			public boolean hasNext()
			{
				return iterator.hasNext();
			}

			public Iterator<? extends T> next()
			{
				return iterator.next().iterator();
			}

			public void remove()
			{
				throw new UnsupportedOperationException();
			}
		};
		return Iterators.concat(iterators);
	}

	@Override
	public boolean addAll(Collection<? extends T> set)
	{
		if (set.isEmpty())
		{
			return true;
		}
		size += set.size();
		return sets.add(set);
	}

	@Override
	public boolean add(T o)
	{
		return sets.add(Collections.singleton(o));
	};

	@Override
	public int size()
	{
		return size;
	}
}
