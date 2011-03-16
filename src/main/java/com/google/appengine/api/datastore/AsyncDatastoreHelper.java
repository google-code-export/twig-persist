package com.google.appengine.api.datastore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.api.datastore.ReadPolicy.Consistency;
import com.google.appengine.repackaged.com.google.io.protocol.ProtocolMessage;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.DatastorePb;
import com.google.apphosting.api.DatastorePb.GetRequest;
import com.google.apphosting.api.DatastorePb.GetResponse;
import com.google.code.twig.util.FutureAdaptor;
import com.google.code.twig.util.ImmediateFuture;
import com.google.storage.onestore.v3.OnestoreEntity;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;

/**
 * This class is in an appengine package so it has access to package private internals that 
 * are essential to async operations.  There is a danger that if this unpublished code 
 * changes this may break without warning.
 * 
 * @author John Patterson <john@vercer.com>
 */
public class AsyncDatastoreHelper
{
	// flag that makes all async calls actually run sync
	private static boolean runAllSync;
	private static final Logger logger = Logger.getLogger(AsyncDatastoreHelper.class.getName());
	
	public static Future<List<Key>> put(final Transaction txn, final Iterable<Entity> entities)
	{
		DatastorePb.PutRequest request = new DatastorePb.PutRequest();
		if (txn != null)
		{
			TransactionImpl.ensureTxnActive(txn);
			request.setTransaction(DatastoreServiceImpl.localTxnToRemoteTxn(txn));
		}

		for (Entity entity : entities)
		{
			OnestoreEntity.EntityProto proto = EntityTranslator.convertToPb(entity);
			request.addEntity(proto);
		}

		Future<byte[]> futureBytes = makeAsyncCall("Put", request);

		return new FutureAdaptor<byte[], List<Key>>(futureBytes)
		{
			@Override
			protected List<Key> adapt(byte[] bytes)
			{
				try
				{
					DatastorePb.PutResponse response = new DatastorePb.PutResponse();
					if (bytes != null)
					{
						response.mergeFrom(bytes);
				    }
					Iterator<Entity> entitiesIterator = entities.iterator();
					Iterator<Reference> keyReferenceIterator = response.keys().iterator();
					List<Key> keysInOrder = new ArrayList<Key>(response.keySize());
					while (entitiesIterator.hasNext())
					{
						Entity entity = entitiesIterator.next();
						OnestoreEntity.Reference reference = keyReferenceIterator.next();
						KeyTranslator.updateKey(reference, entity.getKey());
						keysInOrder.add(entity.getKey());
					}
	
					return keysInOrder;
				}
				catch (NoSuchMethodError e)
				{
					logger.log(Level.SEVERE, "Problem with async interface", e);
					DatastoreService service = DatastoreServiceFactory.getDatastoreService();
					List<Key> result = service.put(txn, entities);
					return result;
				}
			}
		};
	}
	
	public static Future<Map<Key, Entity>> get(Transaction txn, final Iterable<Key> keys, Consistency consistency)
	{
			return asyncGet(txn, keys, consistency);
	}

	private static Future<Map<Key, Entity>> asyncGet(final Transaction txn, final Iterable<Key> keys,
			Consistency consistency)
	{
		GetRequest request = new GetRequest();
		if (txn != null)
		{
			TransactionImpl.ensureTxnActive(txn);
			request.setTransaction(DatastoreServiceImpl.localTxnToRemoteTxn(txn));
		}
		
		for (Key key : keys)
		{
			request.addKey(KeyTranslator.convertToPb(key));
		}                
		
		if(consistency == Consistency.EVENTUAL)
		{
            request.setFailoverMs(-1L);
		}

		Future<byte[]> futureBytes = makeAsyncCall("Get", request);
		
		return new FutureAdaptor<byte[], Map<Key,Entity>>(futureBytes)
		{
			@Override
			protected Map<Key, Entity> adapt(byte[] bytes)
			{
				try
				{
					Map<Key, Entity> result = new HashMap<Key, Entity>();
					GetResponse response = new GetResponse();
					if (bytes != null)
					{
						response.mergeFrom(bytes);
				    }
					
					Iterator<Key> keyIterator = keys.iterator();
					Iterator<GetResponse.Entity> entityReferenceIterator = response.entitys().iterator();
					while (keyIterator.hasNext())
					{
						Key key = keyIterator.next();
						GetResponse.Entity entityReference = entityReferenceIterator.next();
						if (entityReference.hasEntity())
						{
							result.put(key, EntityTranslator.createFromPb(entityReference.getEntity()));
						}
					}
					
					return result;
				}
				catch (NoSuchMethodError e)
				{
					logger.log(Level.SEVERE, "Problem with async interface", e);
					DatastoreService service = DatastoreServiceFactory.getDatastoreService();
					Map<Key, Entity> result = service.get(txn, keys);
					return result;
				}
			}
		};
	}

	static Future<byte[]> makeAsyncCall(String method, ProtocolMessage<?> request)
	{
		try
		{
			if (!runAllSync)
			{
				return ApiProxy.makeAsyncCall("datastore_v3", method, request.toByteArray());
			}
			else
			{
				return new ImmediateFuture<byte[]>(ApiProxy.makeSyncCall("datastore_v3", method, request.toByteArray()));
			}
		}
		catch (ApiProxy.ApplicationException exception)
		{
			throw DatastoreApiHelper.translateError(exception);
		}
	}

	public static Comparator<Entity> newEntityComparator(List<SortPredicate> sorts)
	{
		return new PreparedMultiQuery.EntityComparator(sorts);
	}
	
	public static void setAlwaysUseSync(boolean runAllSync)
	{
		AsyncDatastoreHelper.runAllSync = runAllSync;
	}
}
