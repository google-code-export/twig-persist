package com.google.code.twig;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Entity;

public interface LoadCommand
{
	<T> TypedLoadCommand<T> type(Class<? extends T> type);
	
	interface TypedLoadCommand<T>
	{
		SingleTypedLoadCommand<T, ?> id(Object id);
		<I> MultipleTypedLoadCommand<T, I, ?> ids(Collection<? extends I> ids);
		<I> MultipleTypedLoadCommand<T, I, ?> ids(I... ids);
	}

	interface CommonLoadCommand<C extends CommonLoadCommand<C>>
	{
		C restrictEntities(Restriction<Entity> restriction);
		C restrictProperties(Restriction<Property> restriction);
		C parent(Object parent);
	}
	
	interface SingleTypedLoadCommand<T, C extends SingleTypedLoadCommand<T, C>> extends CommonLoadCommand<C>, CommandTerminator<T>
	{
	}
	
	interface MultipleTypedLoadCommand<T, I, C extends MultipleTypedLoadCommand<T, I, C>> extends CommonLoadCommand<C>, CommandTerminator<Map<I, T>>
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
		@Override
		Map<I, T> now();
		
		/**
		 * <p>Operates exactly the same as {@link #now()} but runs asynchronously</p>
		 * 
		 * <p>If the command is still running when the servlet request has finished processing
		 * the response will not be returned to the client until the command is finished so that 
		 * any exceptions can be displayed to the client.</p>
		 * 
		 * @return Future used to get instances in same order as ids
		 */
		Future<Map<I, T>> later();
	}
}
