package com.google.appengine.api.datastore;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.appengine.api.datastore.Query.SortPredicate;
import com.google.appengine.repackaged.com.google.io.protocol.ProtocolMessage;
import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.DatastorePb;
import com.google.code.twig.util.FutureAdaptor;
import com.google.storage.onestore.v3.OnestoreEntity;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;
import com.vercer.util.reference.SimpleObjectReference;

/**
 * This class has access to package private internals that are essential to async operations
 * but has the danger that if the internal code is updated this may break without warning.
 * 
 * Use at your own risk.
 * 
 * @author John Patterson <john@vercer.com>
 */
public class AsyncDatastoreHelper
{
	public static Future<List<Key>> put(final Transaction txn, final Iterable<Entity> entities)
	{
		DatastorePb.PutRequest req = new DatastorePb.PutRequest();
		if (txn != null)
		{
			TransactionImpl.ensureTxnActive(txn);
			req.setTransaction(DatastoreServiceImpl.localTxnToRemoteTxn(txn));
		}

		for (Entity entity : entities)
		{
			OnestoreEntity.EntityProto proto = EntityTranslator.convertToPb(entity);
			req.addEntity(proto);
		}

		Future<byte[]> futureBytes = makeAsyncCall("Put", req);

		return new FutureAdaptor<byte[], List<Key>>(futureBytes)
		{
			@Override
			protected List<Key> adapt(byte[] bytes)
			{
				DatastorePb.PutResponse response = new DatastorePb.PutResponse();
				if (bytes != null)
				{
					response.mergeFrom(bytes);
			    }
				Iterator<Entity> entitiesIterator = entities.iterator();
				Iterator<Reference> referenceIterator = response.keys().iterator();
				List<Key> keysInOrder = new ArrayList<Key>(response.keySize());
				while (entitiesIterator.hasNext())
				{
					Entity entity = entitiesIterator.next();
					OnestoreEntity.Reference reference = referenceIterator.next();
					KeyTranslator.updateKey(reference, entity.getKey());
					keysInOrder.add(entity.getKey());
				}

				return keysInOrder;
			}
		};
	}

	static Future<byte[]> makeAsyncCall(String method, ProtocolMessage<?> request)
	{
		try
		{
			return ApiProxy.makeAsyncCall("datastore_v3", method, request.toByteArray());
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
}
