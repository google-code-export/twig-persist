package com.google.code.twig.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.configuration.DefaultConfiguration;
import com.google.code.twig.util.generic.Generics;

public class AnnotationConfiguration extends DefaultConfiguration implements Configuration
{
	private final boolean indexed;

	public AnnotationConfiguration(boolean indexPropertiesDefault, int defaultVersion)
	{
		super(defaultVersion);
		this.indexed = indexPropertiesDefault;
	}

	public AnnotationConfiguration(boolean indexPropertiesDefault)
	{
		this(indexPropertiesDefault, 0);
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

	@SuppressWarnings("deprecation")
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
				if (name.matches(".*this\\$[0-9]+"))
				{
					throw new IllegalStateException("Inner class " + field.getDeclaringClass()
							+ " must be declared static");
				}
				else
				{
					throw new IllegalStateException("Final field " + field + " cannot be stored");
				}
			}

			return true;
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
			return annotation.value();
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
		return field.isAnnotationPresent(Parent.class) || field.isAnnotationPresent(Child.class)
				|| field.isAnnotationPresent(Independent.class);
	}

	public int activationDepth(Field field, int depth)
	{
		Activate annotation = field.getDeclaringClass().getAnnotation(Activate.class);
		if (field.getAnnotation(Activate.class) != null)
		{
			annotation = field.getAnnotation(Activate.class);
		}
		if (annotation != null)
		{
			return annotation.value();
		}
		return depth;
	}

	@Override
	protected int version(java.lang.reflect.Type type)
	{
		Class<?> clazz = Generics.erase(type);
		if (clazz.isAnnotationPresent(Version.class))
		{
			return clazz.getAnnotation(Version.class).value();
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
	public final String name(Field field)
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
}
