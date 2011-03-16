package com.google.appengine.api.datastore;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.apphosting.api.ApiBasePb;
import com.google.apphosting.api.DatastorePb;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.DatastorePb.QueryResult;
import com.google.code.twig.util.FutureAdaptor;
import com.google.code.twig.util.ImmediateFuture;

public class AsyncPreparedQuery extends BasePreparedQuery
{
	private final Query query;
	private final Transaction txn;
	private static final Logger logger = Logger.getLogger(AsyncPreparedQuery.class.getName());

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
		return new FutureAdaptor<byte[], Integer>(fb)
		{
			@Override
			protected Integer adapt(byte[] bytes)
			{
				ApiBasePb.Integer64Proto resp = new ApiBasePb.Integer64Proto();
				resp.mergeFrom(bytes);
				return (int) resp.getValue();
			}
		};
	}

	private Future<QueryResultIterator<Entity>> runAsyncQuery(Query q,
			final FetchOptions fetchOptions)
	{
		DatastorePb.Query queryProto = convertToPb(q, fetchOptions);
		Future<byte[]> futureBytes = AsyncDatastoreHelper.makeAsyncCall("RunQuery", queryProto);

		return new FutureAdaptor<byte[], QueryResultIterator<Entity>>(futureBytes)
		{
			@Override
			protected QueryResultIterator<Entity> adapt(byte[] bytes)
			{
				try
				{
					DatastorePb.QueryResult result = new DatastorePb.QueryResult();
					if (bytes != null)
					{
						result.mergeFrom(bytes);
					}
					Future<QueryResult> future = new ImmediateFuture<DatastorePb.QueryResult>(result);
					
					QueryResultsSourceImpl src = new QueryResultsSourceImpl(new ApiConfig(), fetchOptions,	txn, future);
					return new QueryResultIteratorImpl(AsyncPreparedQuery.this, src, fetchOptions, txn);
				}
				catch (NoSuchMethodError e)
				{
					// revert to sync operation if unpublished interface fails
					logger.log(Level.SEVERE, "Problem with async interface", e);
					DatastoreService service = DatastoreServiceFactory.getDatastoreService();
					PreparedQuery prepared = service.prepare(txn, query);
					QueryResultIterator<Entity> iterator = prepared.asQueryResultIterator();
					return iterator;
				}
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
		throw new UnsupportedOperationException();
	}

	@Override
	public int countEntities(FetchOptions fetchoptions)
	{
		return 0;
	}
}