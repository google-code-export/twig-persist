package com.google.code.twig;

public interface Work<R>
{
	public R perform(ObjectDatastore datastore);
}
