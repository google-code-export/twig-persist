package com.google.code.twig.standard;

import com.google.appengine.api.datastore.Entity;
import com.google.code.twig.annotation.Version;

class StandardCommand
{
	protected final TranslatorObjectDatastore datastore;

	StandardCommand(TranslatorObjectDatastore datastore)
	{
		datastore.command = this;
		
		// check we have the same thread
		if (datastore.thread != Thread.currentThread())
		{
			throw new IllegalStateException("Detected use of ObjectDatastore by more than one thread.");
		}
		
		this.datastore = datastore;
	}

	protected Long version(Entity entity, Class<?> type)
	{
		if (type.isAnnotationPresent(Version.class))
		{
			String name = type.getAnnotation(Version.class).value();
			Object property = entity.getProperty(name);
			if (property != null)
			{
				if (property instanceof Long == false)
				{
					throw new IllegalStateException("Version property must be long but was " + property);
				}
				return (Long) property;
			}
		}
		return null;
	}
}
