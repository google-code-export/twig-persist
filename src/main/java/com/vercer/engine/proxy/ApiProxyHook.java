package com.vercer.engine.proxy;


import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import com.google.apphosting.api.ApiProxy;
import com.google.apphosting.api.ApiProxy.ApiConfig;
import com.google.apphosting.api.ApiProxy.ApiProxyException;
import com.google.apphosting.api.ApiProxy.Delegate;
import com.google.apphosting.api.ApiProxy.Environment;
import com.google.apphosting.api.ApiProxy.LogRecord;

public class ApiProxyHook implements Delegate<Environment>
{
	private Delegate<Environment> baseDelegate;
	private Map<String, Delegate<Environment>> hooks = new HashMap<String, Delegate<Environment>>();

	public ApiProxyHook(Delegate<Environment> base)
	{
		this.baseDelegate = base;
	}

	@SuppressWarnings("unchecked")
	public static ApiProxyHook install()
	{
		ApiProxyHook hook = new ApiProxyHook(ApiProxy.getDelegate());
		ApiProxy.setDelegate(hook);
		return hook;
	}

	public void log(Environment environment, LogRecord record)
	{
		this.baseDelegate.log(environment, record);
	}

	public byte[] makeSyncCall(Environment environment, String packageName, String methodName,
			byte[] request) throws ApiProxyException
	{
		Delegate<Environment> hook = this.hooks.get(packageName);
		if (hook != null)
		{
			return hook.makeSyncCall(environment, packageName, methodName, request);
		}
		else
		{
			return this.baseDelegate.makeSyncCall(environment, packageName, methodName, request);
		}
	}

	public Delegate<Environment> getBaseDelegate()
	{
		return baseDelegate;
	}

	public Map<String, Delegate<Environment>> getHooks()
	{
		return hooks;
	}

	public Future<byte[]> makeAsyncCall(
			Environment environment,
			String packageName,
			String methodName,
			byte[] request,
			ApiConfig config)
	{
		Delegate<Environment> hook = this.hooks.get(packageName);
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