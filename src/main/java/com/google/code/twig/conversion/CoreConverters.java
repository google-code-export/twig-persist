package com.google.code.twig.conversion;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;

import com.vercer.convert.Converter;

public class CoreConverters implements Iterable<Converter<?, ?>>
{
	static DateFormat format = DateFormat.getDateTimeInstance();
	public static class DateToString implements Converter<Date, String>
	{
		public String convert(Date source)
		{
			return format.format(source);
		}
	}
	public static class StringToDate implements Converter<String, Date>
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
	
	public static class ClassToString implements Converter<Class<?>, String>
	{
		public String convert(Class<?> source)
		{
			return source.getName();
		}
	}
	public static class StringToClass implements Converter<String, Class<?>>
	{
		public Class<?> convert(String source)
		{
			try
			{
				return Class.forName(source);
			}
			catch (ClassNotFoundException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	public static class UrltoString implements Converter<URL, String>
	{
		@Override
		public String convert(URL source)
		{
			return source.toExternalForm();
		}
	}
	public static class StringToUrl implements Converter<String, URL>
	{
		@Override
		public URL convert(String source)
		{
			try
			{
				return new URL(source);
			}
			catch (MalformedURLException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	public static class URItoString implements Converter<URI, String>
	{
		@Override
		public String convert(URI source)
		{
			return source.toString();
		}
	}
	public static class StringToURI implements Converter<String, URI>
	{
		@Override
		public URI convert(String source)
		{
			try
			{
				return new URI(source);
			}
			catch (URISyntaxException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}
	
	public static class LocaleToString implements Converter<Locale, String>
	{
		@Override
		public String convert(Locale source)
		{
			if (source == null)
			{
				return null;
			}
			return source.toString();
		}
	}
	public static class StringToLocale implements Converter<String, Locale>
	{
		@Override
		public Locale convert(String source)
		{
			if (source == null) return null;
			
			String[] parts = source.split("_", -1);
			if (parts.length == 3)
			{
				return new Locale(parts[0], parts[1], parts[2]);
			}
			else if (parts.length == 2)
			{
				return new Locale(parts[0], parts[1]);
			}
			else
			{
				return new Locale(parts[0]);
			}
		}
	}

	public static class CurrencyToString implements Converter<Currency, String>
	{
		@Override
		public String convert(Currency source)
		{
			return source.toString();
		}
	}
	public static class StringToCurrency implements Converter<String, Currency>
	{
		@Override
		public Currency convert(String source)
		{
			return Currency.getInstance(source);
		}
	}
	
	public static class EnumSetToLong implements Converter<EnumSet<? extends Enum<?>>, Long>
	{
		@Override
		public Long convert(EnumSet<? extends Enum<?>> source)
		{
			long bits = 0l;
			Iterator<? extends Enum<?>> iterator = source.iterator();
			while (iterator.hasNext())
			{
				Enum<?> next = iterator.next();
				if (next.ordinal() > 63)
				{
					throw new IllegalArgumentException("Enum " + next.getDeclaringClass() + " has more than 64 values");
				}
				bits &= 1 << next.ordinal();
			}
			return bits;
		}
	}

	@Override
	public Iterator<Converter<?, ?>> iterator()
	{
		return Arrays.<Converter<?, ?>>asList(
		new DateToString(),
		new StringToDate(),
		new ClassToString(),
		new StringToClass(),
		new UrltoString(),
		new StringToUrl(),
		new URItoString(),
		new StringToURI(),
		new LocaleToString(),
		new StringToLocale(),
		new CurrencyToString(),
		new StringToCurrency()).iterator();
	}
}
