package com.vercer.engine.proxy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.URL;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.HTTPResponse;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;
import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Environment;

public class RemoteDatastoreProxyDelegate extends DelegatingApiProxyLocal
{
	private URLFetchService urlFetchService;

	public RemoteDatastoreProxyDelegate(ApiProxyLocal delegate)
	{
		super(delegate);
		urlFetchService = URLFetchServiceFactory.getURLFetchService();
	}
	
	@Override
	public Future<byte[]> makeAsyncCall(Environment arg0, String arg1, String arg2, byte[] arg3,
			ApiConfig arg4)
	{
		HTTPRequest request;
		try
		{
			request = createRequest(arg1, arg2, arg3);
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("Could not serialize request", e);
		}
		
		final Future<HTTPResponse> futureResponse = urlFetchService.fetchAsync(request);
		
		return new Future<byte[]>()
		{
			@Override
			public boolean cancel(boolean mayInterruptIfRunning)
			{
				return futureResponse.cancel(mayInterruptIfRunning);
			}

			@Override
			public byte[] get() throws InterruptedException, ExecutionException
			{
				return responseToBytes(futureResponse.get());
			}

			@Override
			public byte[] get(long timeout, TimeUnit unit) throws InterruptedException,
					ExecutionException, TimeoutException
			{
				return responseToBytes(futureResponse.get(timeout, unit));
			}

			@Override
			public boolean isCancelled()
			{
				return futureResponse.isCancelled();
			}

			@Override
			public boolean isDone()
			{
				return futureResponse.isDone();
			}
		};
	}
	
	protected byte[] responseToBytes(HTTPResponse httpResponse)
	{
		return httpResponse.getContent();
	}

	private HTTPRequest createRequest(String arg1, String arg2, byte[] arg3) throws IOException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);

		oos.writeObject(arg1);
		oos.writeObject(arg2);
		oos.writeObject(arg3);
		
		HTTPRequest request = new HTTPRequest(new URL("www.targetrooms.com/datastore"));
		request.setPayload(baos.toByteArray());
		return request;
	}

	@Override
	public byte[] makeSyncCall(Environment arg0, String arg1, String arg2, byte[] arg3)
			throws ApiProxyException
	{
		HTTPResponse response;
		try
		{
			HTTPRequest request = createRequest(arg1, arg2, arg3);
			response = urlFetchService.fetch(request);
		}
		catch (IOException e)
		{
			throw new IllegalStateException("Problem calling remote service", e);
		}
		return responseToBytes(response);
	}
}
