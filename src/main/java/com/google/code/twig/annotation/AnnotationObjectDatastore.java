package com.google.code.twig.annotation;

import com.google.code.twig.Registry;
import com.google.code.twig.Settings;
import com.google.code.twig.standard.StandardObjectDatastore;

public class AnnotationObjectDatastore extends StandardObjectDatastore
{
	private static AnnotationConfiguration configuration;

	public AnnotationObjectDatastore()
	{
		this(true);
	}

	public AnnotationObjectDatastore(boolean indexed)
	{
		super(Settings.defaults().build(), createConfiguration(indexed), getStaticRegistry());
	}

	public AnnotationObjectDatastore(Settings defaults, Registry registry, boolean indexed)
	{
		super(defaults, createConfiguration(indexed), registry);
	}

	private static AnnotationConfiguration createConfiguration(boolean indexed)
	{
		if (configuration != null && configuration.isIndexed() != indexed)
		{
			throw new IllegalArgumentException("Convenience method can only handle one indexed value");
		}

		if (configuration == null)
		{
			configuration = new AnnotationConfiguration(indexed);
		}
		return configuration;
	}
}
