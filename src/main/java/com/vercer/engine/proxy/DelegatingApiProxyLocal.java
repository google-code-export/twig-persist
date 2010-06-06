package com.vercer.engine.proxy;

import java.util.Map;

import com.google.appengine.tools.development.ApiProxyLocal;
import com.google.appengine.tools.development.Clock;
import com.google.appengine.tools.development.LocalRpcService;

public abstract class DelegatingApiProxyLocal extends DelegatingApiProxy implements ApiProxyLocal
{
	protected DelegatingApiProxyLocal(ApiProxyLocal delegate)
	{
		super(delegate);
	}

	@Override
	public ApiProxyLocal getDelegate()
	{
		return (ApiProxyLocal) super.getDelegate();
	}

	@Override
	public Clock getClock()
	{
		return getDelegate().getClock();
	}

	@Override
	public LocalRpcService getService(String s)
	{
		return getDelegate().getService(s);
	}

	@Override
	public void setClock(Clock clock)
	{
		getDelegate().setClock(clock);
	}

	@Override
	public void setProperties(Map<String, String> arg0)
	{
		getDelegate().setProperties(arg0);
	}

	@Override
	public void setProperty(String s, String s1)
	{
		getDelegate().setProperty(s, s1);
	}

	@Override
	public void stop()
	{
		getDelegate().stop();
	}
}
