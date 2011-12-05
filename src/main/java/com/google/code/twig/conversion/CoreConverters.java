package com.google.code.twig.conversion;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Currency;
import java.util.Date;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.Locale;

import com.google.code.twig.util.generic.Generics;

public class CoreConverters
{
	public static void registerAll(CombinedConverter converter)
	{
		converter.append(new DateToString());
		converter.append(new StringToDate());
		converter.append(new ClassToString());
		converter.append(new StringToClass());
		converter.append(new UrltoString());
		converter.append(new StringToUrl());
		converter.append(new URItoString());
		converter.append(new StringToURI());
		converter.append(new LocaleToString());
		converter.append(new StringToLocale());
		converter.append(new CurrencyToString());
		converter.append(new StringToCurrency());
	}

	public static class DateToString implements SpecificConverter<Date, String>
	{
		public String convert(Date source)
		{
			DateFormat format = DateFormat.getDateTimeInstance();
			return format.format(source);
		}
	}
	public static class StringToDate implements SpecificConverter<String, Date>
	{
		public Date convert(String source)
		{
			try
			{
				DateFormat format = DateFormat.getDateTimeInstance();
				return format.parse(source);
			}
			catch (ParseException e)
			{
				throw new IllegalStateException(e);
			}
		}
	}

	public static class ClassToString implements SpecificConverter<Class<?>, String>
	{
		public String convert(Class<?> source)
		{
			return source.getName();
		}
	}
	public static class StringToClass implements SpecificConverter<String, Class<?>>
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

	public static class UrltoString implements SpecificConverter<URL, String>
	{
		@Override
		public String convert(URL source)
		{
			return source.toExternalForm();
		}
	}
	public static class StringToUrl implements SpecificConverter<String, URL>
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

	public static class URItoString implements SpecificConverter<URI, String>
	{
		@Override
		public String convert(URI source)
		{
			return source.toString();
		}
	}
	public static class StringToURI implements SpecificConverter<String, URI>
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

	public static class LocaleToString implements SpecificConverter<Locale, String>
	{
		@Override
		public String convert(Locale source)
		{
			return source.toString();
		}
	}
	public static class StringToLocale implements SpecificConverter<String, Locale>
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

	public static class CurrencyToString implements SpecificConverter<Currency, String>
	{
		@Override
		public String convert(Currency source)
		{
			return source.toString();
		}
	}
	public static class StringToCurrency implements SpecificConverter<String, Currency>
	{
		@Override
		public Currency convert(String source)
		{
			return Currency.getInstance(source);
		}
	}

	public static class EnumSetToLong implements SpecificConverter<EnumSet<? extends Enum<?>>, Long>
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

	public static class LongToEnumSet implements TypeConverter
	{
		@SuppressWarnings("unchecked")
		@Override
		public <T> T convert(Object source, Type type)
		{
			if (source instanceof Long
					&& type instanceof ParameterizedType
					&& EnumSet.class.isAssignableFrom(Generics.erase(type)))
			{
				Long value = (Long) source;
				Type enumSetType = Generics.getExactSuperType(type, EnumSet.class);
				Type enumType = ((ParameterizedType) enumSetType).getActualTypeArguments()[0];

				@SuppressWarnings("rawtypes")  // not sure how to define enum class safely
				Class<? extends Enum> enumClass = (Class<? extends Enum>) Generics.erase(enumType);

				EnumSet<?> result = bitsToEnums(value, enumClass);

				return (T) result;
			}
			else
			{
				return null;
			}
		}

		protected <E extends Enum<E>> EnumSet<?> bitsToEnums(Long value, Class<E> enumClass)
		{
			EnumSet<E> all = EnumSet.allOf(enumClass);
			EnumSet<E> result = EnumSet.noneOf(enumClass);
			for (E current : all)
			{
				// see if the bit is set in the ordinal position
				if ((value & 1 << current.ordinal()) > 0)
				{
					result.add(current);
				}
			}
			return result;
		}
	}

}
