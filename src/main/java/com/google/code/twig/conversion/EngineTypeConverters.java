package com.google.code.twig.conversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.google.code.twig.conversion.CoreTypeConverters.DateToString;
import com.google.code.twig.conversion.CoreTypeConverters.StringToDate;
import com.google.code.twig.util.io.NoDescriptorObjectInputStream;
import com.google.code.twig.util.io.NoDescriptorObjectOutputStream;

public class EngineTypeConverters
{
	public static void registerAll(CombinedTypeConverter converter)
	{
		converter.register(new StringToText());
		converter.register(new TextToString());
		
		converter.register(new StringToDate());
		converter.register(new DateToString());

		converter.register(new ByteArrayToBlob());
		converter.register(new BlobToByteArray());

		converter.register(new FastSerializableToBlob());
		converter.register(new FastBlobToAnything());
	}
	
	public static class StringToText implements SpecificTypeConverter<String, Text>
	{
		public Text convert(String source)
		{
			return new Text(source);
		}
	}
	
	public static class TextToString implements SpecificTypeConverter<Text, String>
	{
		public String convert(Text source)
		{
			return source.getValue();
		}
	}

	public static class ByteArrayToBlob implements SpecificTypeConverter<byte[], Blob>
	{
		public Blob convert(byte[] source)
		{
			return new Blob(source);
		}
	}

	public static class BlobToByteArray implements SpecificTypeConverter<Blob, byte[]>
	{
		public byte[] convert(Blob source)
		{
			return source.getBytes();
		}
	}

	public static class FastSerializableToBlob implements SpecificTypeConverter<Serializable, Blob>
	{
		public Blob convert(Serializable source)
		{
			try
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
				ObjectOutputStream stream = createObjectOutputStream(baos);
				stream.writeObject(source);
				return new Blob(baos.toByteArray());
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}

		protected ObjectOutputStream createObjectOutputStream(ByteArrayOutputStream baos) throws IOException
		{
			return new NoDescriptorObjectOutputStream(baos);
		}

	}
	
	public static class SerializableToBlob extends FastSerializableToBlob
	{
		@Override
		protected ObjectOutputStream createObjectOutputStream(ByteArrayOutputStream baos) throws IOException
		{
			return new ObjectOutputStream(baos);
		}
	}
	
	public static class FastBlobToAnything implements TypeConverter
	{
		public Object convert(Blob blob)
		{
			try
			{
				ByteArrayInputStream bais = new ByteArrayInputStream(blob.getBytes());
				ObjectInputStream stream = createObjectInputStream(bais);
				return stream.readObject();
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}

		protected ObjectInputStream createObjectInputStream(ByteArrayInputStream bais) throws IOException
		{
			return new NoDescriptorObjectInputStream(bais);
		}

		@SuppressWarnings("unchecked")
		public <T> T convert(Object source, Type type)
		{
			if (source != null && source.getClass() == Blob.class)
			{
				return (T) convert((Blob) source);
			}
			return null;
		}
	}
	
	public static class BlobToAnything extends FastBlobToAnything
	{
		@Override
		protected ObjectInputStream createObjectInputStream(ByteArrayInputStream bais) throws IOException
		{
			return new ObjectInputStream(bais);
		}
	}
}
