package com.google.code.twig.annotation;

import com.google.code.twig.Settings;
import com.google.code.twig.standard.StandardObjectDatastore;

public class AnnotationObjectDatastore extends StandardObjectDatastore
{
	public AnnotationObjectDatastore()
	{
		this(true);
	}

	public AnnotationObjectDatastore(boolean indexed)
	{
		this(Settings.builder().build(), indexed);
	}

	public AnnotationObjectDatastore(Settings settings)
	{
		this(Settings.builder().build(), 0, false);
	}
	
	public AnnotationObjectDatastore(Settings settings, boolean indexed)
	{
		this(Settings.builder().build(), 0, indexed);
	}
	
	public AnnotationObjectDatastore(Settings settings, int activation, boolean indexed)
	{
		super(settings, new AnnotationConfiguration(), activation, indexed);
	}
}
