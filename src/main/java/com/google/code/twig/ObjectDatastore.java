package com.google.code.twig;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Transaction;
import com.google.code.twig.StoreCommand.MultipleStoreCommand;
import com.google.code.twig.StoreCommand.SingleStoreCommand;

/**
 * @author John Patterson <john@vercer.com>
 *
 */
public interface ObjectDatastore extends Activator
{
	/**
	 * <p>Starts a method chain to store instances in the datastore. This is a
	 * more flexible approach than the convenience store methods which do not offer
	 * all the options available here. The method chain must be terminated with
	 * one of the .return* methods which actually send the data to the datastore.</p>
	 *
	 * <p>The methods {@link SingleStoreCommand#later()} and {@link MultipleStoreCommand#later()}
	 * do not block and return a {@link Future} object immediately which allows your
	 * application to continue to do other work in parallel while waiting for the datastore to store
	 * your data. If you require the Keys from these operations you should call {@link Future#get()}
	 * which will block until the datastore has stored the entities and returned the Keys.
	 * Any exceptions that occurred during the execution of the command will be re-thrown when
	 * you make this call.</p>
	 *
	 * <p>If a non-blocking asynchronous command is still running when you have finished processing the
	 * servlet request, the response will not be returned to the client until the async command is
	 * finished so that any exceptions can be displayed.</p>
	 *
	 * @return StoreCommand for precise control of storing instances
	 */
	StoreCommand store();

	/**
	 * @return
	 */
	FindCommand find();

	/**
	 * @return
	 */
	LoadCommand load();

	// convenience store methods

	Key store(Object instance);
	Key store(Object instance, String id);
	Key store(Object instance, long id);
	<T> Map<T, Key> storeAll(Collection<? extends T> instances);

	/**
	 * Update this persistent instance in the datastore. A check is made to ensure that this instance is
	 * @param instance
	 */
	void update(Object instance);
	void updateAll(Collection<?> instances);
	void storeOrUpdate(Object instance);
	void storeOrUpdateAll(Collection<?> instances);

	// convenience load methods
	<T> T load(Key key);
	<T> T load(Class<? extends T> type, Object id);
	<T> Map<?, T> loadAll(Class<? extends T> type, Collection<?> ids);

	// convenience find methods
	<T> QueryResultIterator<T> find(Class<? extends T> type);
	<T> QueryResultIterator<T> find(Class<? extends T> type, String field, Object value);

	// convenience delete methods
	void delete(Object instance);
	void deleteAll(Class<?> type);
	void deleteAll(Collection<?> instances);
	void deleteKey(Key key);
	void deleteKeys(Collection<Key> key);

	/**
	 * Refresh an associated instance with the latest version from the datastore
	 * @param instance The instance to refresh from datastore
	 * @throws IllegalArgumentException if the instance is not associated
	 */
	void refresh(Object instance);


	/**
	 * Refreshes all associated instances with the latest version from the datastore
	 * @param instances The instances to refresh from the datastore
	 * @throws IllegalArgumentException if any of the instances is not associated
	 */
	void refreshAll(Collection<?> instances);

	// cache control operations

	/**
	 * Creates a {@link Key} using this instances @Id and @Parent fields and puts
	 * the key in the internal key cache. If no id field is declared you must use
	 * {@link #associate(Object, Key)} instead. The instance will be in the
	 * activated. Referenced instances will <b>not</b> be activated which is different
	 * from the way this method worked in version 1.0.
	 *
	 * @param instance The persistent instance to be associated with this ObjectDatastore
	 */

	// associate all referenced instances because it is dangerous to have an
	// associated instance reference an unassociated instance

	// these must return the current associated instance because the procedure
	// must examine all referenced instances to associate so it is not possible
	// to have a method that returns the key of a dummy instance and then choose
	// to associate or not depending on if it is already associated.

	<T> T associate(T instance);
	<T> T associate(T instance, boolean activated);
	<T> T associate(T instance, boolean activated, Object parent);


	// TODO this could be confused with other methods - best to make a method chain
	<T> T associate(T instance, long version, Object parent, Object id);

	<T> Collection<T> associateAll(Collection<T> instances);

	/**
	 * Adds this instance but not other referenced instances to the internal
	 * key cache so they are known by this datastore as persistent instances.
	 * If an id field is also defined it will be ignored and the given key
	 * used instead.
	 *
	 * @param instance The root of the object graph to add to the key cache
	 * @param key The Key which is associated with this instance
	 */
	<T> T associate(T instance, Key key);
	<T> T associate(T instance, Key key, long version);


	<T> T associate(Class<T> type, long id);

	<T> T associate(Class<T> type, String id);


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
	

	<T> T associatedInstance(Key key);

	boolean isAssociated(Object instance);

	// transactions
	Transaction beginTransaction();
	Transaction getTransaction();
	void transact(Runnable runnable);

	void startBatchMode();
	void stopBatchMode();
	void flushBatchedOperations();

	DatastoreService getDefaultService();

	long version(Object instance);
}
