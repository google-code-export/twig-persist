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
import com.google.storage.onestore.v3.OnestoreEntity;
import com.google.storage.onestore.v3.OnestoreEntity.Reference;
import com.vercer.util.reference.SimpleObjectReference;

public class AsyncDatastoreHelper
{
	public static Future<List<Key>> put(final Transaction txn, final Iterable<Entity> entities)
	{
		final DatastorePb.PutRequest req = new DatastorePb.PutRequest();

		final SimpleObjectReference<Future<byte[]>> futureBytes = new SimpleObjectReference<Future<byte[]>>();
		new TransactionRunner(txn, false)  // never auto-commit
		{
			@Override
			protected void run()
			{
				if (txn != null)
				{
					req.setTransaction(DatastoreServiceImpl.localTxnToRemoteTxn(txn));
				}

				for (Entity entity : entities)
				{
					OnestoreEntity.EntityProto proto = EntityTranslator.convertToPb(entity);
					req.addEntity(proto);
				}

				futureBytes.set(makeAsyncCall("Put", req));
			}
		}.runInTransaction();


		return new Future<List<Key>>()
		{
			public boolean isDone()
			{
				return futureBytes.get().isDone();
			}

			public boolean isCancelled()
			{
				return futureBytes.get().isCancelled();
			}

			public List<Key> get(long timeout, TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException
			{
				return doGet(futureBytes.get().get(timeout, unit));
			}

			public List<Key> get() throws InterruptedException, ExecutionException
			{
				return doGet(futureBytes.get().get());
			}

			private List<Key> doGet(byte[] bytes)
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

			public boolean cancel(boolean mayInterruptIfRunning)
			{
				return futureBytes.get().cancel(mayInterruptIfRunning);
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
