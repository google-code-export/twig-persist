/**
 *
 */
package com.google.code.twig.conversion;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;

import com.google.code.twig.util.generic.GenericTypeReflector;

/**
 * Handles conversions from any Collection type or Array to either a HashSet,
 * ArrayList or Array.
 * 
 * Generic Collections and Arrays will be converted into the correct generic
 * Array type.
 * 
 * To add a conversion to a more specific Collection type you will need to add a
 * SpecificTypeConverter<S, T>
 * 
 * @author John Patterson <john@vercer.com>
 */
public class CollectionConverter implements TypeConverter
{
	private final TypeConverter delegate;

	public CollectionConverter(TypeConverter delegate)
	{
		this.delegate = delegate;
	}

	@Override
	public <T> T convert(Object source, Type type)
	{
		// attempt to convert input into an array
		Object[] items = null;
		if (source instanceof Collection<?>)
		{
			items = ((Collection<?>) source).toArray();
		}
		else if (source instanceof Object[])
		{
			items = (Object[]) source;
		}
		else if (source.getClass().isArray())
		{
			// must be a primitive array
			Class<?> componentType = source.getClass().getComponentType();
			Class<?> wrapper = PrimitiveConverter.getWrapperClassForPrimitive(componentType);
			int length = Array.getLength(source);
			items = (Object[]) Array.newInstance(wrapper, length);
			for (int i = 0; i < length; i++)
			{
				items[i] = Array.get(source, i);
			}
		}

		if (items == null)
		{
			// signal we did not handle the conversion by returning null
			return null;
		}
		else
		{
			// get the item type so we can convert them
			Class<?> erased = GenericTypeReflector.erase(type);
			Type componentType = null;
			if (type instanceof GenericArrayType)
			{
				// we need a generic array like Provider<Twig>[]
				componentType = ((GenericArrayType) type).getGenericComponentType();
			}
			else if (erased.isArray())
			{
				// we need an array like Twig[]
				componentType = erased.getComponentType();
			}
			else if (Collection.class.isAssignableFrom(erased))
			{
				// we need some type of collection like Set<Twig>
				Type exact = GenericTypeReflector.getExactSuperType(type, Collection.class);
				componentType = ((ParameterizedType) exact).getActualTypeArguments()[0];
			}
			else
			{
				throw new IllegalArgumentException("Unsupported collection type " + type);
			}

			// convert all the items
			List<Object> convertedItems = new ArrayList<Object>(items.length);
			for (Object item : items)
			{
				if (item != null)
				{
					item = delegate.convert(item, componentType);
					if (item == null)
					{
						throw new IllegalStateException("Could not convert list item " + item
								+ " to " + componentType);
					}
				}
				convertedItems.add(item);
			}

			return this.<T> createCollectionInstance(erased, componentType, convertedItems);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	protected <T> T createCollectionInstance(Class<?> erased, Type componentType,
			List<Object> convertedItems)
	{
		// convert the list of items to the required collection type
		if (erased.isAssignableFrom(HashSet.class))
		{
			T result = (T) new HashSet<Object>(convertedItems);
			return result;
		}
		else if (erased.isAssignableFrom(ArrayList.class))
		{
			T result = (T) convertedItems;
			return result;
		}
		else if (erased.isAssignableFrom(EnumSet.class))
		{
			EnumSet enumSet = EnumSet.noneOf((Class<? extends Enum>) componentType);
			for (Object item : convertedItems)
			{
				Enum<?> e;
				if (item instanceof Enum<?> == false)
				{
					Enum<?> tmp = Enum.valueOf((Class<? extends Enum>) erased, (String) item);
					e = tmp;
				}
				else
				{
					e = (Enum<?>) item;
				}
				enumSet.add(e);
			}
			return (T) enumSet;
		}
		else if (erased.isArray())
		{
			Class<?> arrayClass = GenericTypeReflector.erase(componentType);
			Object[] array = (Object[]) Array.newInstance(arrayClass, convertedItems.size());
			T result = (T) convertedItems.toArray(array);
			return result;
		}
		else
		{
			return null;
		}
	}
}