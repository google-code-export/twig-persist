/**
 *
 */
package com.google.code.twig.translator;

import java.lang.reflect.Type;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.code.twig.conversion.TypeConverter;
import com.google.code.twig.util.generic.GenericTypeReflector;

public final class NativeDirectTranslator extends DirectTranslator
{
	public NativeDirectTranslator(TypeConverter converter)
	{
		super(converter);
	}
	
	@Override
	protected boolean isDirectType(Type type)
	{
		// get the non-parameterised class
		Class<?> erased = GenericTypeReflector.erase(type);
		return erased.isPrimitive() || DataTypeUtils.isSupportedType(erased);
	}
}