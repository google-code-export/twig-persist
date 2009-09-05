package com.vercer.engine.persist.annotation;

import com.google.appengine.api.datastore.DatastoreService;
import com.vercer.engine.persist.StrategyTypesafeSession;
import com.vercer.engine.persist.conversion.DefaultTypeConverters;
import com.vercer.engine.persist.conversion.TypeConverters;
import com.vercer.engine.persist.strategy.DefaultNameStrategy;
import com.vercer.engine.persist.strategy.NamingStrategy;

public class AnnotationTypesafeSession extends StrategyTypesafeSession
{
	private static final TypeConverters CONVERTERS = new DefaultTypeConverters();

	public AnnotationTypesafeSession(DatastoreService datastore)
	{
		this(datastore, true);
	}

	public AnnotationTypesafeSession(DatastoreService datastore, boolean indexed)
	{
		this(datastore, new AnnotationStrategy(indexed));
	}

	public AnnotationTypesafeSession(DatastoreService datastore, NamingStrategy names)
	{
		this(datastore, new AnnotationStrategy(true), names);
	}

	private AnnotationTypesafeSession(DatastoreService datastore, AnnotationStrategy strategy)
	{
		super(datastore, strategy, strategy, new DefaultNameStrategy(), CONVERTERS);
	}

	private AnnotationTypesafeSession(DatastoreService datastore, AnnotationStrategy strategy, NamingStrategy names)
	{
		super(datastore, strategy, strategy, new DefaultNameStrategy(), CONVERTERS);
	}
}
