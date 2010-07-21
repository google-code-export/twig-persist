package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Map;

import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;

public interface ObjectDatastore extends Activator
{
	// fluent style methods
	StoreCommand store();
	FindCommand find();
	LoadCommand load();

	// convenience store methods
	Key store(Object instance);
	Key store(Object instance, String id);
	Key store(Object instance, long id);
	Key store(Object instance, Object parent);
	
	<T> Map<T, Key> storeAll(Collection<? extends T> instances);
	<T> Map<T, Key> storeAll(Collection<? extends T> instances, Object parent);

	// updating
	void update(Object instance);
	void storeOrUpdate(Object instance);
	void storeOrUpdate(Object instance, Object parent);

	// convenience load methods
	<T> T load(Key key);
	<T> T load(Class<T> type, Object key);
	<T> T load(Class<T> type, Object key, Object parent);
	<I, T> Map<I, T> loadAll(Class<? extends T> type, Collection<I> ids);
	
	// convenience find methods
	<T> QueryResultIterator<T> find(Class<T> type);
	<T> QueryResultIterator<T> find(Class<T> type, String field, Object value);

	// convenience delete methods
	void delete(Object instance);
	void deleteAll(Type type);
	void deleteAll(Collection<?> instances);

	// activation
	int getActivationDepth();
	void setActivationDepth(int depth);
	
	/**
	 * Refresh an associated instance with the latest version from the datastore 
	 */
	void refresh(Object instance);
	void refreshAll(Collection<?> instances);
	
	// cache control operations
	
	/**
	 * Adds this instance and any other referenced instances to the internal
	 * key cache so they are known by this datastore as persistent instances.
	 * The instance must define an id field with a valid value. If no id field
	 * is defined you must use {@link #associate(Object, Key)} instead. 
	 * 
	 * @param instance The root of the object graph to add to the key cache
	 */
	void associate(Object instance);
	
	/**
	 * Adds this instance and any other referenced instances to the internal
	 * key cache so they are known by this datastore as persistent instances.
	 * If an id field is also defined it will override the key parameter.
	 * 
	 * @param instance The root of the object graph to add to the key cache
	 * @param key The Key which is associated with this instance
	 */
	void associate(Object instance, Key key);
	
	
	/**
	 * Removes only this instance from the key cache and not any referenced
	 * instances. If an instance is disassociated and then stored it will create
	 * a new entity in the datastore unless an id field is defined.
	 * 
	 * It is not necessary to call this method to release memory because instances
	 * are stored using weak references so when they are garbage collected the
	 * associated key will also be removed.
	 *  
	 * @param instance The instance to remove from the key cache
	 */
	void disassociate(Object instance);
	
	
	/**
	 * Like {@link #disassociate(Object)} but removes every instance and key which
	 * has the effect of resetting the datastore.
	 */
	void disassociateAll();
	
	
	/**
	 * @param instance A persistence instance that is associated with this datastore
	 * @return The Key that is associated with this instance.
	 */
	Key associatedKey(Object instance);

	/**
	 * Sets the configuration to use for all datastore operations. This is useful to
	 * adjust the read policy to choose EVENTUAL or STRONG consistency. 
	 * 
	 * @param config The configuration to use for all datastore operations
	 */
	void setConfiguration(DatastoreServiceConfig config);

	// transactions
	Transaction beginTransaction();
	Transaction getTransaction();
	
}
