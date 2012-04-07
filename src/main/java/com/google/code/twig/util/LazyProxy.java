package com.google.code.twig.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;


public abstract class LazyProxy<T>
{
	private static final class LaxyInvocationHandler<T> implements InvocationHandler
	{
		private T instance;
		private final LazyProxy<T> lazyProxy;

		public LaxyInvocationHandler(LazyProxy<T> lazyProxy)
		{
			this.lazyProxy = lazyProxy;
		}

		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
		{
			if (instance == null)
			{
				instance = lazyProxy.newInstance();
			}
			return method.invoke(instance, args);
		}
	}

	private final Class<?> interfaceClass;

	public LazyProxy(Class<?> interfaceClass)
	{
		this.interfaceClass = interfaceClass;
	}

	@SuppressWarnings("unchecked")
	public T newProxy()
	{
		InvocationHandler handler = new LaxyInvocationHandler<T>(this);
		return (T) Proxy.newProxyInstance(this.getClass().getClassLoader(), new Class[] {interfaceClass}, handler);
	}

	protected abstract T newInstance();

	@SuppressWarnings("unchecked")
	public static <T> T getInstance(T proxy)
	{
		LaxyInvocationHandler<T> handler = (LaxyInvocationHandler<T>) Proxy.getInvocationHandler(proxy);
		return handler.instance;
	}
}
