package com.google.code.twig.conversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Iterator;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.google.code.twig.conversion.CoreConverters.DateToString;
import com.google.code.twig.conversion.CoreConverters.StringToDate;
import com.google.code.twig.util.io.NoDescriptorObjectInputStream;
import com.google.code.twig.util.io.NoDescriptorObjectOutputStream;
import com.vercer.convert.Converter;

public class EngineConverters implements Iterable<Converter<?, ?>>
{
	public static class StringToText implements Converter<String, Text>
	{
		public Text convert(String source)
		{
			if (source == null) return null;
			return new Text(source);
		}
	}

	public static class TextToString implements Converter<Text, String>
	{
		public String convert(Text source)
		{
			if (source == null) return null;
			return source.getValue();
		}
	}

	public static class ByteArrayToBlob implements Converter<byte[], Blob>
	{
		public Blob convert(byte[] source)
		{
			return new Blob(source);
		}
	}

	public static class BlobToByteArray implements Converter<Blob, byte[]>
	{
		public byte[] convert(Blob source)
		{
			return source.getBytes();
		}
	}

	public static class ObjectToBlob implements Converter<Object, Blob>
	{
		public Blob convert(Object source)
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
			return new ObjectOutputStream(baos);
		}

	}

	public static class NoDescriptorSerializableToBlob extends ObjectToBlob
	{
		@Override
		protected ObjectOutputStream createObjectOutputStream(ByteArrayOutputStream baos) throws IOException
		{
			return new NoDescriptorObjectOutputStream(baos);
		}
	}

	public static class BlobToAnything implements Converter<Blob, Object>
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
			return new ObjectInputStream(bais);
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

	public static class NoDescriptorBlobToAnything extends BlobToAnything
	{
		@Override
		protected ObjectInputStream createObjectInputStream(ByteArrayInputStream bais) throws IOException
		{
			return new NoDescriptorObjectInputStream(bais);
		}
	}

	@Override
	public Iterator<Converter<?, ?>> iterator()
	{
		return Arrays.<Converter<?, ?>>asList(
		new StringToText(),
		new TextToString(),
		new StringToDate(),
		new DateToString(),
		new ByteArrayToBlob(),
		new BlobToByteArray(),
		new ObjectToBlob(),
		new BlobToAnything()).iterator();
	}
}
