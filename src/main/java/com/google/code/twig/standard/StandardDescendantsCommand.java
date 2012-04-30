package com.google.code.twig.standard;

import java.util.Iterator;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.code.twig.FindCommand.DescendantsCommand;

public class StandardDescendantsCommand extends StandardDecodeCommand<StandardDescendantsCommand> implements DescendantsCommand 
{
	private final Object ancestor;

	StandardDescendantsCommand(Object ancestor, TranslatorObjectDatastore datastore, int initialActivationDepth)
	{
		super(datastore, initialActivationDepth);
		this.ancestor = ancestor;
	}

	public Iterator<Object> now()
	{
		Key key = datastore.associatedKey(ancestor);
		Query query = new Query(key);
		PreparedQuery prepared = datastore.servicePrepare(query, datastore.getDefaultSettings());
		Iterator<Entity> entities = prepared.asIterator();
		return entitiesToInstances(entities, null);
	}
}
