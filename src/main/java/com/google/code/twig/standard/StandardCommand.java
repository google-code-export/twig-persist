package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;

import com.google.appengine.api.datastore.Entity;
import com.google.code.twig.Path;
import com.google.code.twig.annotation.Version;
import com.google.code.twig.util.Pair;
import com.google.code.twig.util.Reflection;
import com.google.code.twig.util.Strings;
import com.google.code.twig.util.generic.Generics;

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
