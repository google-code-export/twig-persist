package com.vercer.engine.persist.translator;

import java.io.Serializable;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.appengine.api.datastore.Blob;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.conversion.DefaultTypeConverter;
import com.vercer.engine.persist.conversion.DefaultTypeConverter.BlobToAnything;
import com.vercer.engine.persist.conversion.DefaultTypeConverter.SerializableToBlob;
import com.vercer.engine.persist.util.SimpleProperty;


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
