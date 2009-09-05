package com.vercer.engine.persist.annotation;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import com.vercer.engine.persist.strategy.RelationshipStrategy;
import com.vercer.engine.persist.strategy.StorageStrategy;

public class AnnotationStrategy implements RelationshipStrategy, StorageStrategy
{
	private final boolean indexed;

	public AnnotationStrategy(boolean indexed)
	{
		this.indexed = indexed;
	}
	
	public boolean child(Field field)
	{
		Entity annotation = field.getAnnotation(Entity.class);
		if (annotation == null)
		{
			annotation = field.getType().getAnnotation(Entity.class);
		}
		
		if (annotation != null)
		{
			return annotation.value() == Entity.Relationship.CHILD;
		}
		else
		{
			return false;
		}
	}

	public boolean parent(Field field)
	{
		Entity annotation = field.getAnnotation(Entity.class);
		if (annotation == null)
		{
			annotation = field.getType().getAnnotation(Entity.class);
		}
		
		if (annotation != null)
		{
			return annotation.value() == Entity.Relationship.PARENT;
		}
		else
		{
			return false;
		}
	}
	
	public boolean component(Field field)
	{
		Component annotation = field.getAnnotation(Component.class);
		if (annotation != null)
		{
			return annotation.value();
		}
		else
		{
			return false;
		}
	}

	public boolean stored(Field field)
	{
		Stored annotation = field.getAnnotation(Stored.class);
		if (annotation != null)
		{
			return annotation.value();
		}
		else
		{
			return true;
		}
	}
	
	public boolean indexed(Field field)
	{
		Indexed annotation = field.getAnnotation(Indexed.class);
		if (annotation != null)
		{
			return annotation.value();
		}
		else
		{
			return indexed;
		}
	}

	public boolean key(Field field)
	{
		return field.isAnnotationPresent(Key.class);
	}

	public java.lang.reflect.Type typeOf(Field field)
	{
		Type annotation = field.getAnnotation(Type.class);
		if (annotation == null)
		{
			return field.getGenericType();
		}
		else
		{
			return annotation.value();
		}
	}

	public boolean polyMorphic(Field field)
	{
		Component annotation = field.getAnnotation(Component.class);
		if (annotation != null)
		{
			return annotation.polymorphic();
		}
		else
		{
			return !Modifier.isFinal(field.getClass().getModifiers());
		}
	}

	public boolean entity(Field field)
	{
		if (field.isAnnotationPresent(Entity.class))
		{
			return true;
		}
		else
		{
			return field.getType().isAnnotationPresent(Entity.class);
		}
	}
}
