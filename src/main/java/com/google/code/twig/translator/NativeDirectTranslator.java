/**
 *
 */
package com.google.code.twig.translator;

import java.lang.reflect.Type;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.google.code.twig.util.generic.GenericTypeReflector;

public final class NativeDirectTranslator extends DirectTranslator
{
	@Override
	protected boolean isDirectType(Type type)
	{
		// get the non-parameterised class
		Class<?> clazz = GenericTypeReflector.erase(type);
		return clazz.isPrimitive() || DataTypeUtils.isSupportedType(clazz);
	}
}