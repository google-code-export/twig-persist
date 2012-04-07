package com.google.code.twig;

import java.util.HashMap;
import java.util.Map;

public class Registry
{
	// not thread safe as intended to be configured at startup
	private Map<Class<?>, Registration> modelToRegistration = new HashMap<Class<?>, Registry.Registration>();
	
	public static class Registration
	{
		private String name;

		public Registration(String name)
		{
			this.name = name;
		}
		
		public String getName()
		{
			return name;
		}
	}

	// caches are configured statically during startup by 
	public void register(Class<?> model)
	{
		Registration registration = new Registration(null);
		modelToRegistration.put(model, registration);
	}
	
	public Registration getRegistration(Class<?> model)
	{
		return modelToRegistration.get(model);
	}
}
