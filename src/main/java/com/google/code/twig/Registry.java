package com.google.code.twig;

import java.util.HashMap;
import java.util.Map;

import com.google.code.twig.annotation.Entity;

public class Registry
{
	// not thread safe as intended to be configured at startup
	private Map<Class<?>, Registration> classToRegistration = new HashMap<Class<?>, Registry.Registration>();
	private Map<String, Registration> nameToRegistration = new HashMap<String, Registry.Registration>();
	
	private final Annotator annotator;
	
	public static class Registration
	{
		private String name;
		private final Class<?> model;

		public Registration(String name, Class<?> model)
		{
			this.name = name;
			this.model = model;
		}
		
		public String getName()
		{
			return name;
		}
		
		public Class<?> getModel()
		{
			return model;
		}
	}
	
	public Registry(Annotator annotator)
	{
		this.annotator = annotator;
	}

	// caches are configured statically during startup by 
	public void register(Class<?> model)
	{
		Entity annotation = annotator.annotation(model, Entity.class);
		if (annotation.kind().isEmpty())
		{
			throw new IllegalArgumentException("Model classes must be annotated with a kind name");
		}
		Registration registration = new Registration(annotation.kind(), model);
		classToRegistration.put(model, registration);
	}

	public Registration registration(Class<?> model)
	{
		return classToRegistration.get(model);
	}
	
	public Registration registration(String kind)
	{
		return nameToRegistration.get(kind);
	}
}
