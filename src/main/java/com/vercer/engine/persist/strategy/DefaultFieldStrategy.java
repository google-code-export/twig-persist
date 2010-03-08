package com.vercer.engine.persist.strategy;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.vercer.engine.persist.util.generic.GenericTypeReflector;

/**
 * @author John Patterson <john@vercer.com>
 *
 */
public class DefaultFieldStrategy implements FieldStrategy
{
	private final int defaultVersion;

	public DefaultFieldStrategy(int defaultVersion)
	{
		this.defaultVersion = defaultVersion;
	}

	protected Type nameToType(String name)
	{
		try
		{
			name = name.replace('_', '.');
			return Class.forName(name);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException(e);
		}
	}

	private final static Pattern pattern = Pattern.compile("v\\d_");
	public final Type kindToType(String name)
	{
		Matcher matcher = pattern.matcher(name);
		if (matcher.lookingAt())
		{
			name = name.substring(matcher.end());
		}
		return nameToType(name);
	}

	/**
	 *
	 * @see com.vercer.engine.persist.strategy.FieldStrategy#name(java.lang.reflect.Field)
	 */
	public final String name(Field field)
	{
		String name = field.getName();
		if (name.charAt(0) == '_')
		{
			name = name.substring(1);
		}
		return name;
	}

	public final String typeToKind(Type type)
	{
		String kind = typeToName(type);
		if (kind.indexOf('_') > 0)
		{
			throw new IllegalArgumentException("Illegal character '_' in class name " + kind);
		}
		int version = version(type);
		if (version > 0)
		{
			kind = "v" + version + "_" + kind;
		}
		kind = kind.replace('.', '_');
		return kind;
	}

	protected String typeToName(Type type)
	{
		Class<?> clazz = GenericTypeReflector.erase(type);
		String kind = clazz.getName();
		return kind;
	}

	protected int version(Type type)
	{
		return defaultVersion;
	}

	/**
	 * Replaces all Collection types and Arrays with List. Converts all elements of
	 * the collection to the component type.
	 *
	 * @see com.vercer.engine.persist.strategy.FieldStrategy#typeOf(java.lang.reflect.Field)
	 */
	public Type typeOf(Field field)
	{
		return replace(field.getGenericType());
	}

	protected Type replace(Type type)
	{
		// turn every collection or array into a list
		Type componentType = null;
		Class<?> erased = GenericTypeReflector.erase(type);
		if (type instanceof GenericArrayType)
		{
			// we have a generic array like Provider<Twig>[]
			GenericArrayType arrayType = (GenericArrayType) type;
			componentType = arrayType.getGenericComponentType();
		}
		else if (erased.isArray())
		{
			// we have a normal array like Twig[]
			componentType = erased.getComponentType();
		}
		else if (Collection.class.isAssignableFrom(erased))
		{
			// we have some kind of collection like Set<Twig>
			Type exact = GenericTypeReflector.getExactSuperType(type, Collection.class);
			componentType = ((ParameterizedType) exact).getActualTypeArguments()[0];
		}
		else
		{
			// we have a non-collection type
			return type;
		}

		// we have a collection type so need to convert it

		// recurse in case we have e.g. List<Twig[]>
		final Type replaced = replace(componentType);

		// replace the collection type with a list type
		return new ParameterizedType()
		{
			public Type getRawType()
			{
				return List.class;
			}

			public Type getOwnerType()
			{
				return null;
			}

			public Type[] getActualTypeArguments()
			{
				return new Type[] { replaced };
			}
		};
	}



}
