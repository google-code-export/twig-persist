package com.google.code.twig;

import com.google.code.twig.annotation.AnnotationConfiguration;
import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.standard.StandardObjectDatastore;

public class ObjectDatastoreFactory
{
	private static ObjectDatastoreFactory instance = new ObjectDatastoreFactory();
	
	private Registry registry;
	private Annotator annotator = new Annotator();
	private Settings settings = Settings.defaults().build();
	private Configuration configuration = new AnnotationConfiguration(true);
	
	/**
	 * @return A new ObjectDatastore
	 */
	public static ObjectDatastore createObjectDatastore()
	{
		return instance.create(instance.getSettings(), instance.getConfiguration(), instance.getRegistry());
	}
	
	protected ObjectDatastore create(Settings settings, Configuration configuration, Registry registry)
	{
		return new StandardObjectDatastore(settings, configuration, registry);
	}
	
	// TODO should be impossible to register during normal use
	public static void register(Class<?> model)
	{
		instance.registry.register(model);
	}
	
	public static void setInstance(ObjectDatastoreFactory instance)
	{
		ObjectDatastoreFactory.instance = instance;
	}
	
	public static ObjectDatastoreFactory getInstance()
	{
		return instance;
	}

	public void setConfiguration(Configuration configuration)
	{
		this.configuration = configuration;
	}

	public Configuration getConfiguration()
	{
		return configuration;
	}

	public void setRegistry(Registry registry)
	{
		this.registry = registry;
	}
	
	public void setAnnotator(Annotator annotator)
	{
		this.annotator = annotator;
	}

	public synchronized Registry getRegistry()
	{
		if (registry == null)
		{
			registry = new Registry(annotator);
		}
		return registry;
	}
	
	public Settings getSettings()
	{
		return settings;
	}
	
	public void setSettings(Settings settings)
	{
		this.settings = settings;
	}
}
