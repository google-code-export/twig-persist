package com.google.code.twig.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class ImmediateFuture<T> implements Future<T>
{
	private final T result;

	public ImmediateFuture(T result)
	{
		this.result = result;
	}
	
	public boolean cancel(boolean mayInterruptIfRunning)
	{
		return false;
	}

	public T get() throws InterruptedException, ExecutionException
	{
		return result;
	}

	public T get(long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException
	{
		return result;
	}

	public boolean isCancelled()
	{
		return false;
	}

	public boolean isDone()
	{
		return false;
	}
}
