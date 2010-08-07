package com.google.code.twig.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class SortedMergeIterator<T> extends AbstractIterator<T>
{
	private final Comparator<T> comparator;
	private PriorityQueue<PeekingIterator<T>> peekings;
	private T last;

	public SortedMergeIterator(Comparator<T> comparator, Collection<Iterator<T>> iterators)
	{
		this.comparator = comparator;
		peekings = new PriorityQueue<PeekingIterator<T>>(iterators.size(), pc);
		for (Iterator<T> iterator : iterators)
		{
			if (iterator.hasNext())
			{
				// make a peeking iterator so we can sort by its top element
				PeekingIterator<T> peeking = Iterators.peekingIterator(iterator);
				peekings.offer(peeking);
			}
		}
	}

	@Override
	protected T computeNext()
	{
		// keep getting elements until we have a non-duplicate
		T next = null;
		do
		{
			PeekingIterator<T> top = peekings.poll();
			
			// take the top iterator
			if (top == null)
			{
				// time to go home
				return endOfData();
			}
			
			// take the top element from the top iterator
			next = top.next();
			
			if (top.hasNext())
			{
				// re-sort the iterator now its top element has changed
				peekings.offer(top);
			}
		}
		while (last != null && comparator.compare(next, last) == 0);

		last = next;
		
		return next;
	}

	Comparator<PeekingIterator<T>> pc = new Comparator<PeekingIterator<T>>()
	{
		public int compare(PeekingIterator<T> o1, PeekingIterator<T> o2)
		{
			int compare = comparator.compare(o1.peek(), o2.peek());
			return compare;
		}
	};


}
