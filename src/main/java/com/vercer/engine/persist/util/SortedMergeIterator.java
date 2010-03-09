package com.vercer.engine.persist.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;

import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.PeekingIterator;

public class SortedMergeIterator<T> extends AbstractIterator<T>
{
	private final Comparator<T> comparator;
	private LinkedList<PeekingIterator<T>> peekings;
	private final boolean dedup;

	public SortedMergeIterator(Comparator<T> comparator, Collection<Iterator<T>> iterators, boolean ignoreDuplicates)
	{
		this.comparator = comparator;
		this.dedup = ignoreDuplicates;
		peekings = new LinkedList<PeekingIterator<T>>();
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

		T next = removeTop();

		// discard duplicates
		if (dedup)
		{
			while (peekings.size() > 0 && peekings.getFirst().peek().equals(next))
			{
				removeTop();
			}
		}

		return next;
	}

	private T removeTop()
	{
		PeekingIterator<T> top = peekings.getFirst();
		T next = top.next(); // step forward the top iterator
		if (!top.hasNext())
		{
			peekings.removeFirst();
		}
		Collections.sort(peekings, pc);
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
