package com.vercer.engine.persist.strategy;

public interface CombinedStrategy extends ActivationStrategy, CacheStrategy, FieldStrategy, RelationshipStrategy,StorageStrategy
{
}
