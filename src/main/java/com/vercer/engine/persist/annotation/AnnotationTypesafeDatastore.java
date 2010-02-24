package com.vercer.engine.persist.annotation;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.vercer.engine.persist.StrategyTypesafeDatastore;
import com.vercer.engine.persist.strategy.FieldTypeStrategy;

public class AnnotationTypesafeDatastore extends StrategyTypesafeDatastore
{
	public AnnotationTypesafeDatastore()
	{
		this(DatastoreServiceFactory.getDatastoreService());
	}
	
	public AnnotationTypesafeDatastore(DatastoreService datastore)
	{
		this(datastore, true);
	}

	public AnnotationTypesafeDatastore(DatastoreService datastore, boolean indexed)
	{
		this(datastore, new AnnotationStrategy(indexed));
	}

	public AnnotationTypesafeDatastore(DatastoreService datastore, FieldTypeStrategy fields)
	{
		this(datastore, new AnnotationStrategy(true), fields);
	}

	protected AnnotationTypesafeDatastore(DatastoreService datastore, AnnotationStrategy strategy, FieldTypeStrategy fields)
	{
		super(datastore, strategy, strategy, fields);
	}
}
