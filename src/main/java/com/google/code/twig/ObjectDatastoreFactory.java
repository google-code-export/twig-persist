package com.google.code.twig;

import com.google.code.twig.annotation.AnnotationConfiguration;
import com.google.code.twig.standard.StandardObjectDatastore;

public class ObjectDatastoreFactory
{
	public static ObjectDatastore createObjectDatastore()
	{
		return createObjectDatastore(Settings.builder().build());
	}
	
	public static ObjectDatastore createObjectDatastore(Settings settings)
	{
		return new StandardObjectDatastore(settings, new AnnotationConfiguration(), 0, false);
	}
}
