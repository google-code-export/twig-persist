package com.google.code.twig;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;

public interface LoadCommand
{
	<T> TypedLoadCommand<T> type(Class<? extends T> type);
	SingleUntypedLoadCommand key(Key key);
//	SingleUntypedLoadCommand<?> key(Key key);
	MultipleUntypedLoadCommand keys(Collection<Key> key);
	
	interface TypedLoadCommand<T>
	{
		SingleTypedLoadCommand<T> id(Object id);
		MultipleTypedLoadCommand<T> ids(Collection<?> ids);
	}

	public enum CacheMode { OFF, ON, AUTO, BYPASS };
	
	interface CommonDecodeCommand<C extends CommonDecodeCommand<C>>
	{
		C restrictEntities(Restriction<Entity> restriction);
		C restrictProperties(Restriction<Property> restriction);
		C activate(int depth);
		C activateAll();
		C consistency(Consistency consistency);
		C deadline(long value, TimeUnit unit);
		C cache(CacheMode mode);
	}
	
	interface CommonLoadCommand<C extends CommonLoadCommand<C>> extends CommonDecodeCommand<C>
	{
		C parent(Object parent);
	}

	interface SingleTypedLoadCommand<T> extends CommonLoadCommand<SingleTypedLoadCommand<T>>, CommandTerminator<T>
	{
	}

	interface SingleUntypedLoadCommand extends CommonDecodeCommand<SingleUntypedLoadCommand>
	{
		<T> T now();
	}
	
	interface MultipleUntypedLoadCommand extends CommonDecodeCommand<MultipleUntypedLoadCommand>
	{
		<T> Map<Key, T> now();
	}
	
	interface MultipleTypedLoadCommand<T> extends CommonLoadCommand<MultipleTypedLoadCommand<T>>, CommandTerminator<Map<Object, T>>
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
		Map<Object, T> now();
		
		/**
		 * <p>Operates exactly the same as {@link #now()} but runs asynchronously</p>
		 * 
		 * <p>If the command is still running when the servlet request has finished processing
		 * the response will not be returned to the client until the command is finished so that 
		 * any exceptions can be displayed to the client.</p>
		 * 
		 * @return Future used to get instances in same order as ids
		 */
		Future<Map<Object, T>> later();
	}
}
