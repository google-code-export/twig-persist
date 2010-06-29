package com.google.appengine.api.datastore;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.appengine.api.utils.FutureWrapper;
import com.google.apphosting.api.ApiBasePb;
import com.google.apphosting.api.DatastorePb;
import com.google.apphosting.api.ApiProxy.ApiConfig;

public class AsyncPreparedQuery extends BasePreparedQuery
{
	private final Query query;
	private final Transaction txn;

	public AsyncPreparedQuery(Query query, Transaction txn)
	{
		this.query = query;
		this.txn = txn;
	}

	public Future<QueryResultIterator<Entity>> asFutureQueryResultIterator()
	{
		return asFutureQueryResultIterator(FetchOptions.Builder.withDefaults());
	}

	public Future<QueryResultIterator<Entity>> asFutureQueryResultIterator(FetchOptions fetchOptions)
	{
		if (fetchOptions.getCompile() == null)
		{
			fetchOptions = new FetchOptions(fetchOptions).compile(true);
		}
		return runAsyncQuery(this.query, fetchOptions);
	}

	public Future<Integer> countEntitiesAsync()
	{
		DatastorePb.Query queryProto = convertToPb(this.query, FetchOptions.Builder.withDefaults());

		Future<byte[]> fb = AsyncDatastoreHelper.makeAsyncCall("Count", queryProto);
		return new FutureWrapper<byte[], Integer>(fb)
		{
			@Override
			protected Throwable convertException(Throwable e)
			{
				return e;
			}

			@Override
			protected Integer wrap(byte[] bytes) throws Exception
			{
				ApiBasePb.Integer64Proto resp = new ApiBasePb.Integer64Proto();
				resp.mergeFrom(bytes);
				return (int) resp.getValue();
			}
		};
	}

	private Future<QueryResultIterator<Entity>> runAsyncQuery(Query q, final FetchOptions fetchOptions)
	{
		DatastorePb.Query queryProto = convertToPb(q, fetchOptions);
		final Future<byte[]> future;
		future = AsyncDatastoreHelper.makeAsyncCall("RunQuery", queryProto);

		return new Future<QueryResultIterator<Entity>>()
		{
			public boolean isDone()
			{
				return future.isDone();
			}

			public boolean isCancelled()
			{
				return future.isCancelled();
			}

			public QueryResultIterator<Entity> get(long timeout, TimeUnit unit)
					throws InterruptedException, ExecutionException, TimeoutException
			{
				byte[] bs = future.get(timeout, unit);
				return makeResult(bs);
			}

			private QueryResultIterator<Entity> makeResult(byte[] bs)
			{
				DatastorePb.QueryResult result = new DatastorePb.QueryResult();
				if (bs != null)
				{
					result.mergeFrom(bs);
				}
				QueryResultsSourceImpl src = new QueryResultsSourceImpl(null, fetchOptions, txn);
				List<Entity> prefetchedEntities = src.loadFromPb(result);
				return new QueryResultIteratorImpl(AsyncPreparedQuery.this, prefetchedEntities,
						src, fetchOptions, txn);
			}

			public QueryResultIterator<Entity> get() throws InterruptedException,
					ExecutionException
			{
				byte[] bs = future.get();
				return makeResult(bs);
			}

			public boolean cancel(boolean mayInterruptIfRunning)
			{
				return future.cancel(mayInterruptIfRunning);
			}
		};
	}

	private DatastorePb.Query convertToPb(Query q, FetchOptions fetchOptions)
	{
		DatastorePb.Query queryProto = QueryTranslator.convertToPb(q, fetchOptions);
		if (this.txn != null)
		{
			TransactionImpl.ensureTxnActive(this.txn);
			queryProto.setTransaction(DatastoreServiceImpl.localTxnToRemoteTxn(this.txn));
		}
		return queryProto;
	}

	@Override
	public String toString()
	{
		return this.query.toString() + ((this.txn != null) ? " IN " + this.txn : "");
	}

	public Iterator<Entity> asIterator(FetchOptions fetchOptions)
	{
		throw new UnsupportedOperationException();
	}

	public List<Entity> asList(FetchOptions fetchOptions)
	{
		throw new UnsupportedOperationException();
	}

	public QueryResultList<Entity> asQueryResultList(FetchOptions fetchOptions)
	{
		throw new UnsupportedOperationException();
	}

	public Entity asSingleEntity() throws TooManyResultsException
	{
		throw new UnsupportedOperationException();
	}

	public QueryResultIterator<Entity> asQueryResultIterator(FetchOptions fetchOptions)
	{
		throw new UnsupportedOperationException();
	}

	public int countEntities()
	{
		// TODO Auto-generated method stub
		return 0;
	}
}