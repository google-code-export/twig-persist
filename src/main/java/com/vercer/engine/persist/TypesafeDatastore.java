package com.vercer.engine.persist;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Predicate;

public interface TypesafeDatastore
{
	Key store(Object instance);
	Key store(Object instance, String name);
	Key store(Object instance, String name, Object parent);
	Key store(Object instance, Object parent);

	List<Key> storeAll(Collection<?> instances);
	List<Key> storeAll(Collection<?> instances, Object parent);
	
	void storeOrUpdate(Object instance);
	void storeOrUpdate(Object instance, Object parent);
	
	<T> T load(Key key);
	<T> T load(Class<T> type, Object key);
	<T> T load(Class<T> type, Object key, Object parent);
	
	void refresh(Object instance);
	
	<T> QueryResultIterator<T> find(Query query);
	<T> QueryResultIterator<T> find(Query query, FindOptions options);
	<T> QueryResultIterator<T> find(Class<T> type);
	<T> QueryResultIterator<T> find(Class<T> type, FindOptions options);
	<T> QueryResultIterator<T> find(Class<T> type, Object parent);
	<T> QueryResultIterator<T> find(Class<T> type, Object parent, FindOptions options);
	
	/**
	 * Executes multiple queries and merges the results into a single iterator.
	 * 
	 * You can set the result to ignore duplicates if the queries can return
	 * the same entities. Queries must be sorted for this to work.
	 * 
	 * If the query contains a sort field then you should supply a comparator so
	 * that the iterator will also respect this ordering. If more than one entity
	 * can have the same sort order and you are filtering duplicates you should 
	 * also sort by Key by adding the Entity.KEY_RESERVED_PROPERTY sort field.
	 * 
	 * @param <T>
	 * @param queries
	 * @param options
	 * @return
	 */
	<T> Iterator<T> find(Collection<Query> queries, MergeFindOptions options);

	void update(Object instance);
	void delete(Object instance);
	void deleteAll(Class<?> type);
	void deleteAll(Collection<?> instances);

	// cache access operations
	void associate(Object instance);
	void associate(Object instance, Key key);
	void disassociate(Object instance);
	void disassociateAll();
	Key associatedKey(Object instance);

	// bridge type-safe to low-level 
	DatastoreService getService();
	Query query(Class<?> type);
	<T> T toTypesafe(Entity entity);

	public static class FindOptions
	{
		private FetchOptions fetchOptions;
		private Predicate<Entity> entityPredicate;
		private Predicate<String> propertyPredicate;
		private boolean returnParent;
		private boolean prefetch;
		
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
		public void setReturnParent(boolean returnParent)
		{
			this.returnParent = returnParent;
		}
		public boolean isReturnParent()
		{
			return returnParent;
		}
		public void setPrefetch(boolean prefetch)
		{
			this.prefetch = prefetch;
		}
		public boolean isPrefetch()
		{
			return prefetch;
		}
	}
	
	public static class MergeFindOptions extends FindOptions
	{
		private boolean filterDuplicates;
		private Comparator<Entity> entityComparator;
		private Comparator<?> instanceComparator;
		
		public void setFilterDuplicates(boolean duplicates)
		{
			this.filterDuplicates = duplicates;
		}
		public boolean isFilterDuplicates()
		{
			return filterDuplicates;
		}
		public void setEntityComparator(Comparator<Entity> entityComparator)
		{
			this.entityComparator = entityComparator;
		}
		public Comparator<Entity> getEntityComparator()
		{
			return entityComparator;
		}
		public void setInstanceComparator(Comparator<?> instanceComparator)
		{
			this.instanceComparator = instanceComparator;
		}
		public Comparator<?> getInstanceComparator()
		{
			return instanceComparator;
		}
	}
	

}
