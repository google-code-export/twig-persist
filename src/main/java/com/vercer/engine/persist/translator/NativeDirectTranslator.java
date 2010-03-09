/**
 *
 */
package com.vercer.engine.persist.translator;

import java.lang.reflect.Type;

import com.google.appengine.api.datastore.DataTypeUtils;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;

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