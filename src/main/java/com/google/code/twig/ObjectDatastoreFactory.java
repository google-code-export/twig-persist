package com.google.code.twig;

import com.google.code.twig.annotation.AnnotationConfiguration;
import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.standard.StandardObjectDatastore;

public class ObjectDatastoreFactory
{
	private static Registry registry = new Registry();
	private static Configuration configuration = new AnnotationConfiguration(true);
	
	public static ObjectDatastore createObjectDatastore()
	{
		return new StandardObjectDatastore(Settings.defaults().build(), getConfiguration(), getRegistry());
	}
	
	public static void register(Class<?> model)
	{
		registry.register(model);
	}

	public static void setConfiguration(Configuration configuration)
	{
		ObjectDatastoreFactory.configuration = configuration;
	}

	public static Configuration getConfiguration()
	{
		return configuration;
	}

	public static void setRegistry(Registry registry)
	{
		ObjectDatastoreFactory.registry = registry;
	}

	public static Registry getRegistry()
	{
		return registry;
	}
}
