package com.vercer.util;
import java.util.Iterator;


/**
 * Performs two common iterator tasks: filtering out certain elements and adapting elements
 * from a delegate iterator;
 * 
 * @author John Patterson <john@vercer.com>
 *
 * @param <Source> The type of the delegate iterator
 * @param <Target> The adapted type
 */
public abstract class IteratorTranslatorFilter<Source, Target> implements Iterator<Target>
{
	private Target next;
	private final Iterator<Source> delegate;
	
	public IteratorTranslatorFilter(Iterator<Source> delegate)
	{
		this.delegate = delegate;
	}
	
	public boolean hasNext()
	{
		if (next == null)
		{
			next = next();
		}
		return next != null;
	}

	public Target next()
	{
		if (next != null)
		{
			Target result = next;
			next = null;
			return result;
		}
		else
		{
			return doNext();
		}
	}

	protected Target doNext()
	{
		while (delegate.hasNext())
		{
			Source source = delegate.next();
			if (include(source))
			{
				return translate(source);
			}
		}
		return null;
	}

	protected abstract boolean include(Source source);

	protected abstract Target translate(Source source);

	public void remove()
	{
		throw new UnsupportedOperationException();
	}
	
}
