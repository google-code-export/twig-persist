package com.google.code.twig.annotation;

import com.google.code.twig.Annotator;
import com.google.code.twig.Registry;
import com.google.code.twig.Settings;
import com.google.code.twig.standard.StandardObjectDatastore;

public class AnnotationObjectDatastore extends StandardObjectDatastore
{
	private static Registry registry;

	public AnnotationObjectDatastore()
	{
		this(true);
	}

	public AnnotationObjectDatastore(boolean indexed)
	{
		super(Settings.defaults().build(), createConfiguration(indexed), getStaticRegistry());
	}

	private synchronized static Registry getStaticRegistry()
	{
		if (registry == null)
		{
			registry = new Registry(new Annotator());
		}
		return registry;
	}
	
	public AnnotationObjectDatastore(Settings settings)
	{
		super(settings, createConfiguration(false), getStaticRegistry());
	}

	public AnnotationObjectDatastore(Settings defaults, Registry registry, boolean indexed)
	{
		super(defaults, createConfiguration(indexed), registry);
	}

	private static AnnotationConfiguration createConfiguration(boolean indexed)
	{
		return new AnnotationConfiguration(indexed);
	}
}
