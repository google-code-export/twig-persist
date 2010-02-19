package com.vercer.engine.persist.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class SortedMergeIterator<T> extends AbstractIterator<T>
{
	private final Comparator<T> comparator;
	
	Comparator<PeekingIterator<T>> pc = new Comparator<PeekingIterator<T>>()
	{
		public int compare(PeekingIterator<T> o1, PeekingIterator<T> o2)
		{
			return comparator.compare(o1.peek(), o2.peek());
		}
	};

	private List<PeekingIterator<T>> peekings;
	private final boolean filter;
	
	public SortedMergeIterator(Comparator<T> comparator, Collection<Iterator<T>> iterators, boolean filter)
	{
		this.comparator = comparator;
		this.filter = filter;
		peekings = new ArrayList<PeekingIterator<T>>();
		for (Iterator<T> iterator : iterators)
		{
			if (iterator.hasNext())
			{
				PeekingIterator<T> peeking = Iterators.peekingIterator(iterator);
				peekings.add(peeking);
			}
		}
		Collections.sort(peekings, pc);
	}

	@Override
	protected T computeNext()
	{
		if (peekings.isEmpty())
		{
			return endOfData();
		}
		
		PeekingIterator<T> top = top();
		T next = top.next();
		topChanged();

		// discard duplicates
		if (filter)
		{
			while (peekings.size() > 0 && top().peek().equals(next))
			{
				top().next();  // skip the current top
				topChanged();
			}
		}
		
		return next;
	}

	private PeekingIterator<T> top()
	{
		return peekings.get(peekings.size() - 1);
	}

	private void topChanged()
	{
		int size = peekings.size();
		PeekingIterator<T> top = top();
		if (top.hasNext())
		{
			T peeked = top.peek();
			int after;
			for (after = size - 2; after >= 0; after--)
			{
				PeekingIterator<T> peeking = peekings.get(after);
				T compare = peeking.peek();
				
				if (comparator.compare(peeked, compare) > 0)
				{
					break;
				}
			}
			
			if (after < size - 2)
			{
				peekings.remove(size - 1);
				peekings.add(after + 1, top);
			}
		}
		else
		{
			peekings.remove(size - 1);
		}
	}

}
