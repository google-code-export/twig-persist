package com.google.code.twig.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.code.twig.conversion.PrimitiveConverter;
import com.google.code.twig.util.generic.Generics;

/**
 * @author John Patterson <john@vercer.com>
 *
 */
public abstract class DefaultConfiguration implements Configuration
{
	private final int defaultVersion;

	private static Map<String, Type> nameToType;
	private static Map<Type,String> typeToName;
	
	public DefaultConfiguration(int defaultVersion)
	{
		this.defaultVersion = defaultVersion;
	}

	public static void registerTypeName(Type type, String name)
	{
		if (nameToType == null)
		{
			nameToType = new ConcurrentHashMap<String, Type>();
			typeToName = new ConcurrentHashMap<Type, String>();
		}
		
		// put the values and check that there was no existing mappings
		Type existingType = nameToType.get(name);
		if (existingType == null)
		{
			nameToType.put(name, type);
		}
		else
		{
			throw new IllegalArgumentException("Kind name " + name + " was already mapped to " + existingType);
		}
		
		String existingName = typeToName.get(type);
		if (existingName == null)
		{
			typeToName.put(type, name);
		}
		else
		{
			throw new IllegalArgumentException("Type " + type + " was already mapped to kind name " + existingName);
		}
	}
	
	/**
	 * Decode a type name - possibly abbreviated - into a type.
	 * 
	 * @param name The portion of the kind name that specifies the Type
	 */
	protected Type nameToType(String name)
	{
		try
		{	
			if (nameToType != null)
			{
				Type type = nameToType.get(name);
				if (type != null)
				{
					return type;
				}
			}
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
		
		//use space as a place holder as it cannot exist in property names
		name = name.replaceAll("__", " ");
		name = name.replace('_', '.');
		name = name.replace(' ', '_');
		return nameToType(name);
	}

	/**
	 *
	 * @see com.google.code.twig.strategy.KeyStrategy#name(java.lang.reflect.Field)
	 */
	public String name(Field field)
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
		
		// replace . with _ and _ with __
		kind = kind.replace('.', ' ');
		kind = kind.replaceAll("_", "__");
		kind = kind.replace(' ', '_');
		
		int version = version(type);
		if (version > 0)
		{
			kind = "v" + version + "_" + kind;
		}
		return kind;
	}

	/**
	 * The converse method {@link #nameToType(String)} must understand how to "decode" type
	 * names encoded by this method
	 * 
	 * @return A representation that can unambiguously specify the type
	 */
	protected String typeToName(Type type)
	{
		if (typeToName != null)
		{
			String name = typeToName.get(type);
			if (name != null)
			{
				return name;
			}
		}
		Class<?> clazz = Generics.erase(type);
		String kind = clazz.getName();
		return kind;
	}

	/**
	 * @return The datastore version to store this type under
	 */
	protected int version(Type type)
	{
		return defaultVersion;
	}

	/**
	 * Replaces all Collection types and Arrays with List. Converts all elements of
	 * the collection to the component type.
	 *
	 * @see com.google.code.twig.strategy.KeyStrategy#typeOf(java.lang.reflect.Field)
	 */
	public Type typeOf(Field field)
	{
		return replaceCollectionWithList(field.getGenericType());
	}

	protected Type replaceCollectionWithList(final Type type)
	{
		// turn every collection or array into an array list
		Type componentType = null;
		Class<?> erased = Generics.erase(type);
		if (type instanceof TypeVariable<?>)
		{
			return erased;
		}
		else if (type instanceof GenericArrayType)
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
		else if (Collection.class.isAssignableFrom(erased) && !ArrayList.class.isAssignableFrom(erased))
		{
			// we have some kind of collection like Set<Twig>
			Type exact = Generics.getExactSuperType(type, Collection.class);
			componentType = ((ParameterizedType) exact).getActualTypeArguments()[0];
		}
		else
		{
			// we have a non-collection type
			return type;
		}

		// we have a collection so convert its component type also
		Type replaced = replaceCollectionWithList(componentType);
		
		// use wrapper type for primitives
		if (Generics.erase(replaced).isPrimitive())
		{
			replaced = PrimitiveConverter.getWrapperClassForPrimitive((Class<?>) replaced);
		}

		// replace the collection type with a list type
		return new ReplacedListType(type, replaced);
	}
	
	private static final class ReplacedListType implements ParameterizedType
	{
		private final Type type;
		private final Type replaced;

		private ReplacedListType(Type type, Type replaced)
		{
			this.type = type;
			this.replaced = replaced;
		}

		public Type getRawType()
		{
			return ArrayList.class;
		}

		public Type getOwnerType()
		{
			return null;
		}

		public Type[] getActualTypeArguments()
		{
			return new Type[] { replaced };
		}

		@Override
		public String toString()
		{
			return "(Replaced " + type + " with List<" + replaced + ">)";
		}
	}
}
