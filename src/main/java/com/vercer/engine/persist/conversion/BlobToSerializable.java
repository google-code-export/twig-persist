package com.vercer.engine.persist.conversion;

import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import com.google.appengine.api.datastore.Blob;

public class BlobToSerializable implements TypeConverter<Blob, Serializable>
{
	public Serializable convert(Blob blob)
	{
		try
		{
			ByteArrayInputStream bais = new ByteArrayInputStream(blob.getBytes());
			ObjectInputStream stream = new ObjectInputStream(bais);
			return (Serializable) stream.readObject();
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
}
