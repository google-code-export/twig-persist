package com.google.code.twig.conversion;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Date;
import java.util.Locale;

public class CoreTypeConverters
{
	public static void registerAll(CombinedTypeConverter converter)
	{
		converter.append(new DateToString());
		converter.append(new StringToDate());
		converter.append(new ClassToString());
		converter.append(new StringToClass());
		converter.append(new UrltoString());
		converter.append(new StringToURI());
		converter.append(new URItoString());
		converter.append(new StringToURI());
		converter.append(new LocaleToString());
		converter.append(new StringToLocale());
		converter.append(new CurrencyToString());
		converter.append(new StringToCurrency());
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
	
	public static class ClassToString implements SpecificTypeConverter<Class<?>, String>
	{
		public String convert(Class<?> source)
		{
			return source.getName();
		}
	}
	public static class StringToClass implements SpecificTypeConverter<String, Class<?>>
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

	public static class UrltoString implements SpecificTypeConverter<URL, String>
	{
		@Override
		public String convert(URL source)
		{
			return source.toExternalForm();
		}
	}
	public static class StringToUrl implements SpecificTypeConverter<String, URL>
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

	public static class URItoString implements SpecificTypeConverter<URI, String>
	{
		@Override
		public String convert(URI source)
		{
			return source.toString();
		}
	}
	public static class StringToURI implements SpecificTypeConverter<String, URI>
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
	
	public static class LocaleToString implements SpecificTypeConverter<Locale, String>
	{
		@Override
		public String convert(Locale source)
		{
			return source.toString();
		}
	}
	public static class StringToLocale implements SpecificTypeConverter<String, Locale>
	{
		@Override
		public Locale convert(String source)
		{
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

	public static class CurrencyToString implements SpecificTypeConverter<Currency, String>
	{
		@Override
		public String convert(Currency source)
		{
			return source.toString();
		}
	}
	public static class StringToCurrency implements SpecificTypeConverter<String, Currency>
	{
		@Override
		public Currency convert(String source)
		{
			return Currency.getInstance(source);
		}
	}
}
