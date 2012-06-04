package com.google.code.twig.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

/**
 * @author John Patterson <jdpatterson@gmail.com>
 */
public abstract class DefaultConfiguration implements Configuration
{
	private static Map<String, Class<?>> nameToType;
	private static Map<Class<?>,String> typeToName;
	
	// cache field types on the fly as the may involve calculation / replacement
//	private static Map<Type, Type> typeToReplacement = Maps.newConcurrentMap();
	
	// TODO remove this
	public static void secretMethodForSneakilyChangingTypeName(Class<?> type, String name)
	{
		nameToType.put(name, type);
		typeToName.put(type, name);
	}

	public static void registerTypeName(Class<?> type, String name)
	{
		if (nameToType == null)
		{
			// should be configured at start-up so no need for concurrency
			nameToType = new HashMap<String, Class<?>>();
			typeToName = new HashMap<Class<?>, String>();
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
		try
		{	
			if (nameToType != null)
			{
				Class<?> type = nameToType.get(name);
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
		if (typeToName != null)
		{
			String name = typeToName.get(type);
			if (name != null)
			{
				return name;
			}
		}
		return type.getName();
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
//		return replace(field.getGenericType());
	}
//	
//	private Type replace(Type type)
//	{
//		// check the cache fist as this could be expensive
//		Type replaced = typeToReplacement.get(type);
//		if (replaced != null) return replaced;
//			
//		Type converted;
//		if (type instanceof ParameterizedType)
//		{
//			ParameterizedType parameterised = (ParameterizedType) type;
//			Type[] arguments = parameterised.getActualTypeArguments();
//			boolean changed = false;
//			for (int i = 0; i < arguments.length; i++)
//			{
//				Type original = arguments[i];
//				
//				// recursively replace type parameters
//				arguments[i] = replace(original);
//				
//				if (original != arguments[i]) changed = true;
//			}
//			
//			Class<?> erased = Generics.erase(type);
//			Class<?> swaped = swap(erased);
//			if (swaped != erased) changed = true;
//			
//			if (changed)
//			{
//				converted = new ParameterizedTypeImpl(swaped, arguments, parameterised.getOwnerType());
//			}
//			else
//			{
//				converted = type;
//			}
//		}
//		else
//		{
//			Type componentType = Generics.getArrayComponentType(type);
//			if (componentType != null)
//			{
//				converted = new ParameterizedTypeImpl(Iterable.class, new Type[] {componentType}, null);
//			}
//			else if (type instanceof Class<?>)
//			{
//				converted = swap((Class<?>) type);
//			}
//			else
//			{
//				// just use the original type
//				converted = type;
//			}
//		}
//		
//		typeToReplacement.put(type, converted);
//		
//		return converted;
//	}
//
//	protected Class<?> swap(Class<?> type)
//	{
////		 store all iterables as lists
//		if (Iterable.class.isAssignableFrom(type))
//		{
//			return List.class;
//		}
//		else
//		{
//			return type;
//		}
//	}
}
