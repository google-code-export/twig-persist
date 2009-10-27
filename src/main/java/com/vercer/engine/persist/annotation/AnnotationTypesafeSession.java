package com.vercer.engine.persist.annotation;

import com.google.appengine.api.datastore.DatastoreService;
import com.vercer.engine.persist.StrategyTypesafeDatastore;
import com.vercer.engine.persist.conversion.DefaultTypeConverter;
import com.vercer.engine.persist.conversion.TypeConverter;
import com.vercer.engine.persist.strategy.FieldTypeStrategy;

public class AnnotationTypesafeSession extends StrategyTypesafeDatastore
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
		super(datastore, strategy, strategy, strategy, CONVERTERS);
	}

	protected AnnotationTypesafeSession(DatastoreService datastore, AnnotationStrategy strategy, FieldTypeStrategy fields)
	{
		super(datastore, strategy, strategy, fields, CONVERTERS);
	}
}
