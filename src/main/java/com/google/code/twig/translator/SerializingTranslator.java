package com.google.code.twig.translator;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.appengine.api.datastore.Blob;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.conversion.DefaultTypeConverter;
import com.google.code.twig.conversion.DefaultTypeConverter.BlobToAnything;
import com.google.code.twig.conversion.DefaultTypeConverter.SerializableToBlob;
import com.google.code.twig.util.SimpleProperty;


public class SerializingTranslator implements PropertyTranslator
{
	private final BlobToAnything blobToSerializable = new DefaultTypeConverter.BlobToAnything();
	private final SerializableToBlob serializableToBlob = new DefaultTypeConverter.SerializableToBlob();

	public final Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
	{
		try
		{
			Property property = properties.iterator().next();
			if (property.getValue() instanceof Blob)
			{
				Blob blob = (Blob) property.getValue();
				return blobToSerializable.convert(blob);
			}
			else
			{
				return null;
			}
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	public final Set<Property> typesafeToProperties(Object object, Path path, boolean indexed)
	{
		try
		{
			if (object instanceof Serializable)
			{
				Blob blob = serializableToBlob.convert((Serializable) object);
				return Collections.singleton((Property) new SimpleProperty(path, blob, indexed));
			}
			else
			{
				return null;
			}
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

}
