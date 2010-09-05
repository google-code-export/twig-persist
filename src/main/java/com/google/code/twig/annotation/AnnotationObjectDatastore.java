package com.google.code.twig.annotation;

import com.google.code.twig.standard.StandardObjectDatastore;

public class AnnotationObjectDatastore extends StandardObjectDatastore
{
	public AnnotationObjectDatastore()
	{
		this(true, 0);
	}
	
	public AnnotationObjectDatastore(boolean indexed, int defaultVersion)
	{
		super(new AnnotationConfiguration(indexed, defaultVersion));
	}

	public AnnotationObjectDatastore(boolean indexed)
	{
		super(new AnnotationConfiguration(indexed, 0));
	}
}
