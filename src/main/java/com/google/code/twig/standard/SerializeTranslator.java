package com.google.code.twig.standard;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.Set;

import com.google.appengine.api.datastore.Blob;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;

public class SerializeTranslator implements PropertyTranslator
{

	private final PropertyTranslator delegate;
	private final int threshold;

	public SerializeTranslator(PropertyTranslator delegate, int threshold)
	{
		this.delegate = delegate;
		this.threshold = threshold;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		if (properties.size() == 1)
		{
			Property property = PropertySets.firstProperty(properties);
			if (property.getPath().equals(path))
			{
				Object value = property.getValue();
				if (value instanceof Blob)
				{
					try
					{
						byte[] bs = ((Blob) value).getBytes();
						ByteArrayInputStream bais = new ByteArrayInputStream(bs);
						ObjectInputStream ois = new ObjectInputStream(bais);
						properties = (Set<Property>) ois.readObject();
					}
					catch (Exception e)
					{
						throw new RuntimeException(e);
					}
				}
			}
		}
		
		return delegate.decode(properties, path, type);
	}

	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		Set<Property> encoded = delegate.encode(instance, path, indexed);
		if (encoded.size() > threshold)
		{
			try
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ObjectOutputStream oos = new ObjectOutputStream(baos);
				oos.writeObject(encoded);
				
				Blob blob = new Blob(baos.toByteArray());
				
				return PropertySets.singletonPropertySet(path, blob, false);
			}
			catch (IOException e)
			{
				throw new RuntimeException(e);
			}
		}
		else
		{
			return encoded;
		}
	}

}
