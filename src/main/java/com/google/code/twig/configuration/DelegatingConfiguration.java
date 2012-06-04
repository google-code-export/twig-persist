package com.google.code.twig.configuration;

import java.lang.reflect.Field;
import java.lang.reflect.Type;

public class DelegatingConfiguration implements Configuration
{
	private final Configuration delegate;

	public int retryNonTransactionPut()
	{
		return this.delegate.retryNonTransactionPut();
	}

	public DelegatingConfiguration(Configuration delegate)
	{
		this.delegate = delegate;
	}

	public int activationDepth(Field field, Integer depth)
	{
		return delegate.activationDepth(field, depth);
	}

	public boolean parent(Field field)
	{
		return delegate.parent(field);
	}

	public String name(Field field)
	{
		return delegate.name(field);
	}

	public boolean child(Field field)
	{
		return delegate.child(field);
	}

	public boolean embed(Field field)
	{
		return delegate.embed(field);
	}
	
	@Override
	public String[] denormalise(Field field)
	{
		return delegate.denormalise(field);
	}

	public String typeToKind(Class<?> type)
	{
		return delegate.typeToKind(type);
	}

	public boolean id(Field field)
	{
		return delegate.id(field);
	}

	public Class<?> kindToType(String kind)
	{
		return delegate.kindToType(kind);
	}

	public boolean entity(Field field)
	{
		return delegate.entity(field);
	}

	public Type typeOf(Field field)
	{
		return delegate.typeOf(field);
	}

	public Boolean index(Field field)
	{
		return delegate.index(field);
	}

	public boolean store(Field field)
	{
		return delegate.store(field);
	}
	
	public boolean polymorphic(Field field)
	{
		return delegate.polymorphic(field);
	}
	
	public int serializationThreshold(Field field)
	{
		return delegate.serializationThreshold(field);
	}

	@Override
	public boolean polymorphic(Class<?> type)
	{
		return delegate.polymorphic(type);
	}
	
	public boolean key(Field field)
	{
		return delegate.key(field);
	}

	@Override
	public long allocateIdsFor(Type type)
	{
		return delegate.allocateIdsFor(type);
	}
	
	public Configuration getDelegate()
	{
		return this.delegate;
	}
	
	@Override
	public String versionPropertyName(Class<?> type)
	{
		return delegate.versionPropertyName(type);
	}
}
