package com.vercer.engine.proxy;

import java.util.concurrent.Future;

import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

public class BaseApiProxyDelegate implements Delegate<Environment>
{
	private final Delegate<Environment> delegate;

	public BaseApiProxyDelegate(Delegate<Environment> delegate)
	{
		this.delegate = delegate;
	}

	public void log(Environment arg0, LogRecord arg1)
	{
		delegate.log(arg0, arg1);
	}

	public Future<byte[]> makeAsyncCall(Environment arg0, String arg1, String arg2, byte[] arg3,
			ApiConfig arg4)
	{
		return delegate.makeAsyncCall(arg0, arg1, arg2, arg3, arg4);
	}

	public byte[] makeSyncCall(Environment arg0, String arg1, String arg2, byte[] arg3)
			throws ApiProxyException
	{
		return delegate.makeSyncCall(arg0, arg1, arg2, arg3);
	}
}
