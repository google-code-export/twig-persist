package com.vercer.convert;

import java.lang.reflect.Type;

public interface TypeConverter
{
	/**
	 * @param instance May not be null
	 * @param target May not be null
	 */
	<T> T convert(Object instance, Type target) throws CouldNotConvertException;

	/**
	 * @param instance May be null
	 * @param source May not be null. Useful when the instance can be null or generics are erased
	 * @param target May not be null
	 */
	<T> T convert(Object instance, Type source, Type target) throws CouldNotConvertException;
	
	public static class CouldNotConvertException extends RuntimeException
	{
		private static final long serialVersionUID = 1L;

		public CouldNotConvertException()
		{
		}

		public CouldNotConvertException(String arg0)
		{
			super(arg0);
		}
	}
}
