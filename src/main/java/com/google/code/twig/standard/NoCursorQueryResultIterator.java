package com.google.code.twig.standard;

import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Index;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.collect.ForwardingIterator;

public class NoCursorQueryResultIterator<T> extends ForwardingIterator<T> implements QueryResultIterator<T>
{
	private final Iterator<T> delegate;

	public NoCursorQueryResultIterator(Iterator<T> delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public Cursor getCursor()
	{
		throw new UnsupportedOperationException("Cursor is not available with cached results");
	}

	@Override
	protected Iterator<T> delegate()
	{
		return delegate;
	}

	@Override
	public List<Index> getIndexList()
	{
		throw new UnsupportedOperationException("Cursor is not available with cached results");
	}
}
