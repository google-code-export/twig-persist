/**
 *
 */
package com.vercer.util.collections;

import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.SortedSet;

import com.google.common.base.Preconditions;
import com.google.common.collect.UnmodifiableIterator;

public class ArraySortedSet<T extends Comparable<T>> extends AbstractSet<T> implements SortedSet<T>
{
	private final T[] elements;
	private final static Comparator<?> comparator = new Comparator<Comparable<Object>>()
	{
		public int compare(Comparable<Object> o1, Comparable<Object> o2)
		{
			return o1.compareTo(o2);
		}
	};
	private final int offset;
	private final int length;

	public ArraySortedSet(T[] elements)
	{
		this(elements, 0, elements.length);
	}

	public ArraySortedSet(T[] elements, int offset, int length)
	{
		this.elements = elements;
		this.offset = offset;
		this.length = length;
	}

	@Override
	public Iterator<T> iterator()
	{
		// copied from non-visible Google Collections Iterators
		final int end = offset + length;
		Preconditions.checkPositionIndexes(offset, end, elements.length);
		return new UnmodifiableIterator<T>()
		{
			int i = offset;

			public boolean hasNext()
			{
				return i < end;
			}

			public T next()
			{
				if (!hasNext())
				{
					throw new NoSuchElementException();
				}
				return elements[i++];
			}
		};
	}

	@Override
	public int size()
	{
		return length;
	}

	@SuppressWarnings("unchecked")
	public Comparator<? super T> comparator()
	{
		return (Comparator<? super T>) comparator;
	}

	public T first()
	{
		return elements[0];
	}

	public SortedSet<T> headSet(T toElement)
	{
		int index = Arrays.binarySearch(elements, toElement);
		return new ArraySortedSet<T>(elements, 0, index + 1);
	}

	public T last()
	{
		return elements[offset + length - 1];
	}

	public SortedSet<T> subSet(T fromElement, T toElement)
	{
		int from = Arrays.binarySearch(elements, fromElement);
		int to = Arrays.binarySearch(elements, toElement);
		return new ArraySortedSet<T>(elements, from, to - from + 1);
	}

	public SortedSet<T> tailSet(T fromElement)
	{
		int index = Arrays.binarySearch(elements, fromElement);
		return new ArraySortedSet<T>(elements, index, length);
	}

}