package com.vercer.engine.async;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

public class ApiProxyHook<E extends Environment> implements Delegate<E>
{
	private Delegate<E> baseDelegate;
	private Map<String, Delegate<E>> hooks = new HashMap<String, Delegate<E>>();

	public ApiProxyHook(Delegate<E> base)
	{
		this.baseDelegate = base;
	}

	public void log(E environment, LogRecord record)
	{
		this.baseDelegate.log(environment, record);
	}

	public byte[] makeSyncCall(E environment, String packageName, String methodName,
			byte[] request) throws ApiProxyException
	{
		Delegate<E> hook = this.hooks.get(packageName);
		if (hook != null)
		{
			return hook.makeSyncCall(environment, packageName, methodName, request);
		}
		else
		{
			return this.baseDelegate.makeSyncCall(environment, packageName, methodName, request);
		}
	}

	public Delegate<E> getBaseDelegate()
	{
		return baseDelegate;
	}

	public Map<String, Delegate<E>> getHooks()
	{
		return hooks;
	}

	public Future<byte[]> makeAsyncCall(
			E environment, 
			String packageName, 
			String methodName,
			byte[] request, 
			ApiConfig config)
	{
		Delegate<E> hook = this.hooks.get(packageName);
		if (hook != null)
		{
			return hook.makeAsyncCall(environment, packageName, methodName, request, config);
		}
		else
		{
			return this.baseDelegate.makeAsyncCall(environment, packageName, methodName, request, config);
		}
	}
}