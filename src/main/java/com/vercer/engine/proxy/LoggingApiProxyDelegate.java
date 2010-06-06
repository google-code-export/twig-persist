package com.vercer.engine.proxy;

import java.util.concurrent.Future;
import java.util.logging.Logger;

import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;

public class LoggingApiProxyDelegate extends DelegatingApiProxy
{
	private static final Logger log = Logger.getLogger(LoggingApiProxyDelegate.class.getName());

	public LoggingApiProxyDelegate(Delegate<Environment> delegate)
	{
		super(delegate);
	}

	@Override
	public byte[] makeSyncCall(Environment arg0, String arg1, String arg2, byte[] arg3)
			throws ApiProxyException
	{
		log.info("Sync " + arg1 + " " + arg2);
		return super.makeSyncCall(arg0, arg1, arg2, arg3);
	}

	@Override
	public Future<byte[]> makeAsyncCall(Environment arg0, String arg1, String arg2, byte[] arg3,
			ApiConfig arg4)
	{
		log.info("Async " + arg1 + " " + arg2);
		return super.makeAsyncCall(arg0, arg1, arg2, arg3, arg4);
	}
}
