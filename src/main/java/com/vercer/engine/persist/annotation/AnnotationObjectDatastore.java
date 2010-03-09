package com.vercer.engine.persist.annotation;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.vercer.engine.persist.standard.StrategyObjectDatastore;
import com.vercer.engine.persist.strategy.FieldStrategy;

public class AnnotationObjectDatastore extends StrategyObjectDatastore
{
	public AnnotationObjectDatastore()
	{
		this(DatastoreServiceFactory.getDatastoreService());
	}

	public AnnotationObjectDatastore(DatastoreService datastore)
	{
		this(datastore, true);
	}

	public AnnotationObjectDatastore(boolean indexed)
	{
		this(DatastoreServiceFactory.getDatastoreService(), indexed);
	}

	public AnnotationObjectDatastore(boolean indexed, int defaultVersion)
	{
		this(DatastoreServiceFactory.getDatastoreService(), new AnnotationStrategy(indexed, defaultVersion));
	}

	public AnnotationObjectDatastore(DatastoreService datastore, boolean indexed)
	{
		this(datastore, new AnnotationStrategy(indexed, 0));
	}

	public AnnotationObjectDatastore(DatastoreService datastore, FieldStrategy fields)
	{
		this(datastore, new AnnotationStrategy(true, 0), fields);
	}

	protected AnnotationObjectDatastore(DatastoreService datastore, AnnotationStrategy strategy, FieldStrategy fields)
	{
		super(datastore, strategy, strategy, strategy, fields);
	}
}
