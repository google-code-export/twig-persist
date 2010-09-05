package com.google.code.twig.strategy;

public interface CombinedStrategy extends ActivationStrategy, CacheStrategy, FieldStrategy,
		RelationshipStrategy, StorageStrategy
{
}
