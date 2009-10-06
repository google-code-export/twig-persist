package com.vercer.engine.persist.annotation;

import com.google.appengine.api.datastore.DatastoreService;
import com.vercer.engine.persist.StrategyTypesafeSession;
import com.vercer.engine.persist.conversion.DefaultTypeConverter;
import com.vercer.engine.persist.conversion.TypeConverter;
import com.vercer.engine.persist.strategy.DefaultFieldTypeStrategy;
import com.vercer.engine.persist.strategy.FieldTypeStrategy;

public class AnnotationTypesafeSession extends StrategyTypesafeSession
{
	private static final TypeConverter CONVERTERS = new DefaultTypeConverter();

	public AnnotationTypesafeSession(DatastoreService datastore)
	{
		this(datastore, true);
	}

	public AnnotationTypesafeSession(DatastoreService datastore, boolean indexed)
	{
		this(datastore, new AnnotationStrategy(indexed));
	}

	public AnnotationTypesafeSession(DatastoreService datastore, FieldTypeStrategy fields)
	{
		this(datastore, new AnnotationStrategy(true), fields);
	}

	protected AnnotationTypesafeSession(DatastoreService datastore, AnnotationStrategy strategy)
	{
		super(datastore, strategy, strategy, new DefaultFieldTypeStrategy(), CONVERTERS);
	}

	protected AnnotationTypesafeSession(DatastoreService datastore, AnnotationStrategy strategy, FieldTypeStrategy names)
	{
		super(datastore, strategy, strategy, new DefaultFieldTypeStrategy(), CONVERTERS);
	}
}
