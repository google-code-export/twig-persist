package com.google.code.twig;

public interface Transactable<R>
{
	public R perform(ObjectDatastore datastore);
}
