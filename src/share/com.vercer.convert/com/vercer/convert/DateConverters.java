package com.vercer.convert;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

/**
 * @author JRodriguez
 * @author John Patterson (john@vercer.com)
 */
public class DateConverters implements Iterable<Converter<?, ?>>
{
	public static class DateLongConverter implements Converter<Date, Long>
	{
		@Override
		public Long convert(Date target)
		{
			return target.getTime();
		}
	}

	public static class LongDateConverter implements Converter<Long, Date>
	{
		@Override
		public Date convert(Long source)
		{
			return new Date(source);
		}
	}

	public static class DateStringConverter implements Converter<Date, String>
	{
		protected DateFormat format;

		public DateStringConverter()
		{
			this.format = DateFormat.getInstance();
		}

		public DateStringConverter(DateFormat format)
		{
			this.format = format;
		}

		public DateStringConverter(String format)
		{
			this.format = new SimpleDateFormat(format);
		}

		@Override
		public String convert(Date target)
		{
			return format.format(target);
		}

		protected DateFormat getFormat()
		{
			return format;
		}
	}

	public static class StringDateConverter implements Converter<String, Date>
	{

		protected DateFormat format;

		public StringDateConverter()
		{
			this.format = DateFormat.getInstance();
		}

		public StringDateConverter(DateFormat format)
		{
			this.format = format;
		}

		public StringDateConverter(String format)
		{
			this.format = new SimpleDateFormat(format);
		}

		@Override
		public Date convert(String source)
		{
			if (source == null || source.isEmpty()) return null;
			try
			{
				return getFormat().parse(source);
			}
			catch (ParseException e)
			{
				throw new IllegalArgumentException("Illegal date format " + source + " expected " + ((SimpleDateFormat) getFormat()).toPattern(), e);
			}
		}

		protected DateFormat getFormat()
		{
			return format;
		}
	}

	public static abstract class LocalizedDateStringConverter extends DateStringConverter
	{

		private int dateStyle;
		private int timeStyle;

		public LocalizedDateStringConverter()
		{
			this(DateFormat.LONG, DateFormat.LONG);
		}

		public LocalizedDateStringConverter(int dateStyle, int timeStyle)
		{
			this.dateStyle = dateStyle;
			this.timeStyle = timeStyle;
		}

		@Override
		protected DateFormat getFormat()
		{
			return DateFormat.getDateTimeInstance(dateStyle, timeStyle, getLocale());
		}

		protected abstract Locale getLocale();
	}

	@Override
	public Iterator<Converter<?, ?>> iterator()
	{
		return Arrays.<Converter<?, ?>>asList(new DateLongConverter(), new LongDateConverter(), new DateStringConverter(), new StringDateConverter()).iterator();
	}
}
