package com.vercer.engine.persist.annotation;

import com.vercer.engine.persist.standard.StandardObjectDatastore;
import com.vercer.engine.persist.strategy.FieldStrategy;

public class AnnotationObjectDatastore extends StandardObjectDatastore
{
	public AnnotationObjectDatastore()
	{
		this(true, 0);
	}
	
	public AnnotationObjectDatastore(boolean indexed, int defaultVersion)
	{
		this(new AnnotationStrategy(indexed, defaultVersion));
	}

	public AnnotationObjectDatastore(boolean indexed)
	{
		this(new AnnotationStrategy(indexed, 0));
	}

	public AnnotationObjectDatastore(FieldStrategy fields)
	{
		this(new AnnotationStrategy(true, 0), fields);
	}

	protected AnnotationObjectDatastore(AnnotationStrategy strategy, FieldStrategy fields)
	{
		super(strategy, strategy, strategy, fields);
	}
}
