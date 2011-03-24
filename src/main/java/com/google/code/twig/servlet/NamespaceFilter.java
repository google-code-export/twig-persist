package com.google.code.twig.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import com.google.appengine.api.NamespaceManager;
import com.google.inject.Singleton;

@Singleton
public class NamespaceFilter implements Filter
{

	@Override
	public void destroy()
	{
	}

	@Override
	public void doFilter(ServletRequest arg0, ServletResponse arg1, FilterChain arg2)
			throws IOException, ServletException
	{
		String server = arg0.getServerName();
		if (server.equals("www.targetrooms.com") || 
				server.equals("localhost") || 
				server.equals("127.0.0.1") || 
				server.endsWith(".local") || 
				server.startsWith("test") || 
				server.startsWith("demo") || 
				server.startsWith("admin-live") || 
				server.startsWith("live"))
		{
			NamespaceManager.set(null);
			arg2.doFilter(arg0, arg1);
		}
		else
		{
			String version = null;
			int index = server.indexOf('.');
			if (index > 0)
			{
				version = server.substring(0, index);
				if (version.startsWith("admin-"))
				{
					version = version.substring(6);
				}
			}
			
			try
			{
				NamespaceManager.set(version);
				arg2.doFilter(arg0, arg1);
			}
			finally
			{
				NamespaceManager.set(null);
			}
		}
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException
	{
	}
}
