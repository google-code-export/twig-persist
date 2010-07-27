package com.google.code.twig;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;

public interface LoadCommand
{
	interface TypedLoadCommand<T>
	{
		SingleTypedLoadCommand<T, ?> id(Object id);
		<K> MultipleTypedLoadCommand<T, K, ?> ids(Collection<K> ids);
		<K> MultipleTypedLoadCommand<T, K, ?> ids(K... ids);
	}

	interface CommonTypedLoadCommand<T, C extends CommonTypedLoadCommand<T, C>>
	{
		C restrictEntities(Restriction<Entity> restriction);
		C restrictProperties(Restriction<Property> restriction);
		C parent(Object parent);
	}
	
	interface SingleTypedLoadCommand<T, C extends SingleTypedLoadCommand<T, C>> extends CommonTypedLoadCommand<T, C>
	{
		T returnResultNow();
		Future<T> returnResultLater();
	}
	
	interface MultipleTypedLoadCommand<T, K, C extends MultipleTypedLoadCommand<T, K, C>> extends CommonTypedLoadCommand<T, C>
	{
		/**
		 * Loads instances in the same order as the ids set with {@link TypedLoadCommand#ids(Collection)}. 
		 * Any ids that do not match a persistent instances are not returned so the result may contain 
		 * less elements than the ids requested. Instances that are already associated with this datastore
		 * will not be refreshed. It is possible for every id to be retrieved from the key cache in which 
		 * case the no datastore calls will be made.
		 *  
		 * @return Instances in same order as ids
		 */
		Map<K, T> returnResultsNow();
		
		/**
		 * <p>Operates exactly the same as {@link #returnResultsNow()} but runs asynchronously</p>
		 * 
		 * <p>If the command is still running when the servlet request has finished processing
		 * the response will not be returned to the client until the command is finished so that 
		 * any exceptions can be displayed to the client.</p>
		 * 
		 * @return Future used to get instances in same order as ids
		 */
		Future<Map<K, T>> returnResultsLater();
	}

	<T> TypedLoadCommand<T> type(Class<T> type);
}
