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

import com.google.appengine.api.datastore.Blob;
import com.google.appengine.api.datastore.Text;

public class DefaultTypeConverter extends CombinedTypeConverter
{
	public DefaultTypeConverter()
	{
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
		if (source.getClass().equals(type))
		{
			return (T) source;
		}

		else
		{
			// use the registered converters
			return (T) super.convert(source, type);
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
	public static class SerializableToBlob implements SpecificTypeConverter<Serializable, Blob>
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
	public static class BlobToSerializable implements SpecificTypeConverter<Blob, Serializable>
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
