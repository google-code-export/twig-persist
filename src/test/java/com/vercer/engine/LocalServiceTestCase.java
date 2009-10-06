package com.vercer.engine;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;

import com.google.appengine.tools.development.ApiProxyLocalImpl;
import com.google.apphosting.api.ApiProxy;

public abstract class LocalServiceTestCase
{

	@Before
	public void setUp() throws Exception
	{
		ApiProxy.setEnvironmentForCurrentThread(new TestEnvironment());
		ApiProxy.setDelegate(new ApiProxyLocalImpl(new File("."))
		{
		});
	}

	@After
	public void tearDown() throws Exception
	{
		// not strictly necessary to null these out but there's no harm either
		ApiProxy.setDelegate(null);
		ApiProxy.setEnvironmentForCurrentThread(null);
	}

	class TestEnvironment implements ApiProxy.Environment
	{
		public String getAppId()
		{
			return "test";
		}

		public String getVersionId()
		{
			return "1.0";
		}

		public String getEmail()
		{
			throw new UnsupportedOperationException();
		}

		public boolean isLoggedIn()
		{
			throw new UnsupportedOperationException();
		}

		public boolean isAdmin()
		{
			throw new UnsupportedOperationException();
		}

		public String getAuthDomain()
		{
			throw new UnsupportedOperationException();
		}

		public String getRequestNamespace()
		{
			return "";
		}

		public Map<String, Object> getAttributes()
		{
			return new HashMap<String, Object>();
		}
	}
}