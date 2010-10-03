package com.google.code.twig.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public abstract class FutureAdaptor<S, T> implements Future<T>
{
	private final Future<S> source;

	public FutureAdaptor(Future<S> source)
	{
		this.source = source;
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		return source.cancel(mayInterruptIfRunning);
	}

	@Override
	public boolean isCancelled()
	{
		return source.isCancelled();
	}

	@Override
	public boolean isDone()
	{
		return source.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException
	{
		return adapt(source.get());
	}

	protected abstract T adapt(S source);

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException,
			TimeoutException
	{
		return adapt(source.get(timeout, unit));
	}
}
