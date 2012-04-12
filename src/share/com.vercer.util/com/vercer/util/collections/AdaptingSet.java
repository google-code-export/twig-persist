package com.vercer.util.collections;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public abstract class AdaptingSet<Target, Source> extends AbstractSet<Target>
{
	private final Set<Source> source;

	public AdaptingSet(Set<Source> source)
	{
		this.source = source;
	}
	
	protected abstract Target wrap(Source source);
	
	protected abstract Source unwrap(Target target);
	
	@Override
	public int size()
	{
		return source.size();
	}

	@Override
	public Iterator<Target> iterator()
	{
		final Iterator<Source> iterator = source.iterator();
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
	public boolean add(Target target)
	{
		return source.add(unwrap(target)); 
	};
	
}
