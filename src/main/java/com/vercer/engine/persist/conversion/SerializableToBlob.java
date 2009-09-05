package com.vercer.engine.persist.conversion;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.google.appengine.api.datastore.Blob;

public class SerializableToBlob implements TypeConverter<Serializable, Blob>
{
	public Blob convert(Serializable source)
	{
		try
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
			ObjectOutputStream stream = new ObjectOutputStream(baos);
			stream.writeObject(source);
			return new Blob(baos.toByteArray());
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

}
