package com.google.code.twig.standard;

import com.google.appengine.api.datastore.Entity;

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
		if (datastore.getTransaction() != null && datastore.getTransaction().isActive() == false)
		{
			datastore.removeTransaction();
		}
	}

	protected Long version(Entity entity, Class<?> type)
	{
		String versionPropertyName = datastore.getConfiguration().versionPropertyName(type);
		if (versionPropertyName != null)
		{
			Object property = entity.getProperty(versionPropertyName);
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
