package com.vercer.engine.persist;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.common.base.Predicate;

public interface TypesafeDatastore
{
	Key store(Object instance);
	Key store(Object instance, String name);
	Key store(Object instance, String name, Object parent);
	Key store(Object instance, Object parent);

	List<Key> storeAll(Iterator<?> instances);
	List<Key> storeAll(Iterator<?> instances, Object parent);
	
	void storeOrUpdate(Object instance);
	void storeOrUpdate(Object instance, Object parent);
	
	<T> T load(Class<T> type, Object key);
	<T> T load(Class<T> type, Object key, Object parent);
	
	<T> Iterator<T> find(Query query);
	<T> Iterator<T> find(Query query, FindOptions options);
	<T> Iterator<T> find(Class<T> type);
	<T> Iterator<T> find(Class<T> type, FindOptions options);
	<T> Iterator<T> find(Class<T> type, Object parent);
	<T> Iterator<T> find(Class<T> type, Object parent, FindOptions options);

	void update(Object instance);
	void delete(Object instance);
	void deleteAll(Class<?> type);
	void deleteAll(Collection<?> instances);

	// cache access operations
	void associate(Object instance);
	void associate(Object instance, Key key);
	void disassociate(Object instance);
	Key associatedKey(Object instance);

	// bridge type-safe to low-level 
	Query query(Class<?> type);
	DatastoreService getService();
	<T> T toTypesafe(Entity entity);
	<T> T keyToInstance(Key key);
	
	 void refresh(Object instance);
	 
	// TODO potential new methods
	//
	// <T> Iterator<T> find(Comparator<T>, Query...)
	//
	// void setDefaultRoot(Object root);
	// void setDataVersion(String version);
    // void disassociateAll();
    // Transaction beginNewOrGetCurrentTransaction();
	
	// TODO not well defined how this works - some values are encoded as multiple 
	// properties so may need to have function that adds required query filters
	// for a particular field of an instance e.g. 
	// void filter("location", new Location(0, 0);
	//
	//	Object encode(Object object);

	public final static class FindOptions
	{
		private FetchOptions fetchOptions;
		private Predicate<Entity> entityPredicate;
		private Predicate<String> propertyPredicate;
		
		public void setFetchOptions(FetchOptions fetchOptions)
		{
			this.fetchOptions = fetchOptions;
		}
		public FetchOptions getFetchOptions()
		{
			return fetchOptions;
		}
		public void setEntityPredicate(Predicate<Entity> entityPredicate)
		{
			this.entityPredicate = entityPredicate;
		}
		public Predicate<Entity> getEntityPredicate()
		{
			return entityPredicate;
		}
		public void setPropertyPredicate(Predicate<String> propertyPredicate)
		{
			this.propertyPredicate = propertyPredicate;
		}
		public Predicate<String> getPropertyPredicate()
		{
			return propertyPredicate;
		}
	}
}
