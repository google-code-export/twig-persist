package com.vercer.engine.persist.conversion;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.engine.persist.util.io.NoDescriptorObjectInputStream;
import com.vercer.engine.persist.util.io.NoDescriptorObjectOutputStream;
import com.vercer.util.Pair;

public class DefaultTypeConverter extends CombinedTypeConverter
{
	private static Map<Pair<Type, Class<?>>, Boolean> superTypes = new ConcurrentHashMap<Pair<Type, Class<?>>, Boolean>();

	public DefaultTypeConverter()
	{
		register(new ByteArrayToBlob());
		register(new BlobToByteArray());

		register(new BlobToSerializable());
		register(new SerializableToBlob());

		register(new StringToText());
		register(new TextToString());

		register(new StringToDate());
		register(new DateToString());

		register(new PrimitiveTypeConverter());
		register(new CollectionConverter(this));
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T convert(Object source, Type type)
	{
		if (source != null && isSuperType(type, source.getClass()))
		{
			return (T) source;
		}
		else
		{
			// use the registered converters
			return (T) super.convert(source, type);
		}
	}


	private static boolean isSuperType(Type type, Class<? extends Object> clazz)
	{
		if (type == clazz)
		{
			return true;
		}

		Pair<Type, Class<?>> key = new Pair<Type, Class<?>>(type, clazz);
		Boolean superType = superTypes.get(key);
		if (superType != null)
		{
			return superType;
		}
		else
		{
			boolean result = GenericTypeReflector.isSuperType(type, clazz);
			superTypes.put(key, result);
			return result;
		}
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

	public static class SerializableToBlob implements SpecificTypeConverter<Serializable, Blob>
	{
		public Blob convert(Serializable source)
		{
			try
			{
				ByteArrayOutputStream baos = new ByteArrayOutputStream(256);
				ObjectOutputStream stream = new NoDescriptorObjectOutputStream(baos);
				stream.writeObject(source);
				return new Blob(baos.toByteArray());
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}

	}
	public static class BlobToSerializable implements SpecificTypeConverter<Blob, Serializable>
	{
		public Serializable convert(Blob blob)
		{
			try
			{
				ByteArrayInputStream bais = new ByteArrayInputStream(blob.getBytes());
				ObjectInputStream stream = new NoDescriptorObjectInputStream(bais);
				return (Serializable) stream.readObject();
			}
			catch (Exception e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	static DateFormat format = DateFormat.getDateTimeInstance();
	public static class DateToString implements SpecificTypeConverter<Date, String>
	{
		public String convert(Date source)
		{
			return format.format(source);
		}
	}
	public static class StringToDate implements SpecificTypeConverter<String, Date>
	{
		public Date convert(String source)
		{
			try
			{
				return format.parse(source);
			}
			catch (ParseException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

}
