package com.google.code.twig.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.regex.Pattern;

import com.google.code.twig.configuration.DefaultConfiguration;
import com.google.code.twig.util.generic.Generics;
import com.vercer.generics.ParameterizedTypeImpl;

public class AnnotationConfiguration extends DefaultConfiguration
{
	private final boolean indexed;
	
	public AnnotationConfiguration(boolean indexPropertiesDefault)
	{
		this.indexed = indexPropertiesDefault;
	}

	public void register(Class<?> type)
	{
		Class<?> tempType = type;
		do
		{
			Entity annotation = type.getAnnotation(Entity.class);
			if (annotation != null && !annotation.kind().equals(""))
			{
				registerTypeName(type, annotation.kind());
			}
			if (!polymorphic(type)) break;
				
			tempType = tempType.getSuperclass();
		}
		while (tempType != Object.class);
	}

	@Override
	public boolean polymorphic(Class<?> instance)
	{
		Entity annotation = instance.getClass().getAnnotation(Entity.class);
		if (annotation != null)
		{
			return annotation.polymorphic();
		}
		return false;
	}
	
	@Override
	public boolean key(Field field)
	{
		return field.isAnnotationPresent(GaeKey.class);
	}

	public boolean child(Field field)
	{
		return field.isAnnotationPresent(Child.class);
	}

	public boolean parent(Field field)
	{
		return field.isAnnotationPresent(Parent.class);
	}

	public boolean embed(Field field)
	{
		Embedded annotation = field.getAnnotation(Embedded.class);
		if (annotation == null)
		{
			annotation = field.getType().getAnnotation(Embedded.class);
		}

		if (annotation != null)
		{
			return annotation.value();
		}
		else
		{
			Embed oldAnnotation = field.getAnnotation(Embed.class);
			if (oldAnnotation == null)
			{
				oldAnnotation = field.getType().getAnnotation(Embed.class);
			}

			if (oldAnnotation != null)
			{
				return oldAnnotation.value();
			}
			else
			{
				return false;
			}
		}
	}
	
	@Override
	public String[] denormalise(Field field)
	{
		Denormalise annotation = field.getAnnotation(Denormalise.class);
		if (annotation == null) 
		{
			return null;
		}
		else
		{
			return annotation.value();
		}
	}

	private static final Pattern innerClassNamePattern = Pattern.compile(".*this\\$[0-9]+");

	public boolean store(Field field)
	{
		Store annotation = field.getAnnotation(Store.class);
		if (annotation != null)
		{
			return annotation.value();
		}
		else
		{
			int modifiers = field.getModifiers();
			if (Modifier.isTransient(modifiers))
			{
				return false;
			}

			if (Modifier.isFinal(modifiers))
			{
				String name = field.getName();
				if (innerClassNamePattern.matcher(name).matches())
				{
					throw new IllegalStateException("Inner class " + field.getDeclaringClass()
							+ " must be declared static");
				}
				else
				{
//					throw new IllegalStateException("Final field " + field + " cannot be stored");
				}
			}

			return true;
		}
	}

	@Override
	public int serializationThreshold(Field field)
	{
		Store annotation = field.getAnnotation(Store.class);
		if (annotation != null)
		{
			return annotation.serializeThreshold();
		}
		else
		{
			return -1;
		}
	}

	public boolean index(Field field)
	{
		Index annotation = field.getDeclaringClass().getAnnotation(Index.class);
		if (field.getAnnotation(Index.class) != null)
		{
			annotation = field.getAnnotation(Index.class);
		}
		if (annotation != null)
		{
			return annotation.value();
		}
		else
		{
			return indexed;
		}
	}

	@SuppressWarnings("deprecation")
	public boolean id(Field field)
	{
		return field.isAnnotationPresent(Key.class) || field.isAnnotationPresent(Id.class);
	}

	@Override
	public java.lang.reflect.Type typeOf(Field field)
	{
		Type annotation = field.getAnnotation(Type.class);
		if (annotation == null)
		{
			return super.typeOf(field);
		}
		else
		{
			if (annotation.parameters().length == 0)
			{
				return annotation.value();
			}
			else
			{
				return new ParameterizedTypeImpl(annotation.value(), annotation.parameters(), null);
			}
		}
	}

	// @Override
	// protected String typeToName(java.lang.reflect.Type type)
	// {
	// Class<?> erased = GenericTypeReflector.erase(type);
	// Entity annotation = erased.getAnnotation(Entity.class);
	// if (annotation != null && annotation.kind().length() > 0)
	// {
	// return annotation.kind();
	// }
	//
	// return super.typeToName(type);
	// }

	public boolean polymorphic(Field field)
	{
		Embedded annotation = field.getAnnotation(Embedded.class);
		if (annotation == null)
		{
			annotation = field.getType().getAnnotation(Embedded.class);
		}

		if (annotation != null)
		{
			return annotation.polymorphic();
		}
		else
		{
			// final classes cannot be polymorphic - all others can
			return !Modifier.isFinal(field.getType().getModifiers());
		}
	}

	public boolean entity(Field field)
	{
		return field.isAnnotationPresent(Parent.class) || 
				field.isAnnotationPresent(Child.class) || 
				field.isAnnotationPresent(Denormalise.class) || 
				field.isAnnotationPresent(Independent.class);
	}

	public int activationDepth(Field field, Integer depth)
	{
		if (field.isAnnotationPresent(Activate.class))
		{
			return field.getAnnotation(Activate.class).value();
		}
		return depth;
	}

	@Override
	protected int version(java.lang.reflect.Type type)
	{
		Class<?> clazz = Generics.erase(type);
		if (clazz.isAnnotationPresent(Entity.class))
		{
			return clazz.getAnnotation(Entity.class).version();
		}
		else
		{
			return super.version(type);
		}
	}

	@Override
	public long allocateIdsFor(java.lang.reflect.Type type)
	{
		Class<?> clazz = Generics.erase(type);
		if (clazz.isAnnotationPresent(Entity.class))
		{
			return clazz.getAnnotation(Entity.class).allocateIdsBy();
		}
		else
		{
			return 0;
		}
	}

	@Override
	public String name(Field field)
	{
		Store annotation = field.getAnnotation(Store.class);
		if (annotation != null && !annotation.name().isEmpty())
		{
			return annotation.name();
		}
		else
		{
			return super.name(field);
		}
	}

	public boolean isIndexed()
	{
		return indexed;
	}
}
