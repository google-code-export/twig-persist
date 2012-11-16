package com.google.code.twig.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author John Patterson <john@vercer.com>
 */
public abstract class DefaultConfiguration implements Configuration
{
	private static Map<String, Class<?>> nameToType = new HashMap<String, Class<?>>();
	private static Map<Class<?>,String> typeToName = new HashMap<Class<?>, String>();
	
	// TODO remove this
	public static void secretMethodForSneakilyChangingTypeName(Class<?> type, String name)
	{
		nameToType.put(name, type);
		typeToName.put(type, name);
	}

	public static void registerTypeName(Class<?> type, String name)
	{
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
	
	@Override
	public int retryNonTransactionPut()
	{
		return 5;
	}
	
	/**
	 * Decode a type name - possibly abbreviated - into a type.
	 * 
	 * @param name The portion of the kind name that specifies the Type
	 */
	protected Class<?> nameToType(String name)
	{
		Class<?> type = nameToType.get(name);
		if (type != null)
		{
			return type;
		}
		else
		{
			throw new IllegalStateException("No type registered for name " + name);
		}
	}

	public final Class<?> kindToType(String name)
	{
		int versionIndex = name.indexOf(':');
		
		if (versionIndex > 0)
		{
			name = name.substring(0, versionIndex);
		}
		
		//use space as a place holder as it cannot exist in property names
		name = name.replaceAll("__", " ");
		name = name.replaceAll("_", ".");
		name = name.replaceAll(" ", "_");
		
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

	public final String typeToKind(Class<?> type)
	{
		String kind = typeToName(type);
		
		// replace . with _ and _ with __
		kind = kind.replace("_", "__");
		kind = kind.replace('.', '_');
		
		int version = version(type);
		if (version > 0)
		{
			kind = kind + ":" + version;
		}
		return kind;
	}

	/**
	 * The converse method {@link #nameToType(String)} must understand how to "decode" type
	 * names encoded by this method
	 * 
	 * @return A representation that can unambiguously specify the type
	 */
	protected String typeToName(Class<?> type)
	{
		String name = typeToName.get(type);
		if (name == null)
		{
			throw new IllegalStateException("Unregistered type " + type);
		}
		return name;
	}

	/**
	 * @return The datastore version to store this type under
	 */
	protected int version(Type type)
	{
		return 0;
	}

	/**
	 * Replaces all Collection types and Arrays with List. Converts all elements of
	 * the collection to the component type.
	 *
	 * @see com.google.code.twig.strategy.KeyStrategy#typeOf(java.lang.reflect.Field)
	 */
	public Type typeOf(Field field)
	{
		return field.getGenericType();
	}

	public static void unregisterAll()
	{
		nameToType.clear();
		typeToName.clear();
	}
}
