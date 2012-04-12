package com.vercer.convert;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ThrowableToString implements Converter<Throwable, String>
{

	@Override
	public String convert(Throwable source)
	{
		StringWriter writer = new StringWriter();
		source.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}

}
