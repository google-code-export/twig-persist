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

import com.google.common.collect.UnmodifiableIterator;

public class ArraySortedSet<T> extends AbstractSet<T> implements SortedSet<T>
{
	private final T[] elements;
	private final Comparator<? super T> comparator;
	
	private final int offset;
	private final int length;

	public ArraySortedSet(T[] elements, Comparator<? super T> comparator)
	{
		this(elements, 0, elements.length, comparator);
	}

	public ArraySortedSet(T[] elements, int offset, int length, Comparator<? super T> comparator)
	{
		this.elements = elements;
		this.offset = offset;
		this.length = length;
		this.comparator = comparator;
	}

	@Override
	public Object[] toArray()
	{
		return elements;
	}
	
	@Override
	public Iterator<T> iterator()
	{
		final int end = offset + length;
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

	public Comparator<? super T> comparator()
	{
		return comparator;
	}

	public T first()
	{
		return elements[0];
	}

	public SortedSet<T> headSet(T toElement)
	{
		int index = Arrays.binarySearch(elements, toElement, comparator);
		return new ArraySortedSet<T>(elements, 0, index + 1, comparator);
	}

	public T last()
	{
		return elements[offset + length - 1];
	}

	public SortedSet<T> subSet(T fromElement, T toElement)
	{
		int from = Arrays.binarySearch(elements, fromElement);
		int to = Arrays.binarySearch(elements, toElement);
		return new ArraySortedSet<T>(elements, from, to - from + 1, comparator);
	}

	public SortedSet<T> tailSet(T fromElement)
	{
		int index = Arrays.binarySearch(elements, fromElement);
		return new ArraySortedSet<T>(elements, index, length, comparator);
	}

}