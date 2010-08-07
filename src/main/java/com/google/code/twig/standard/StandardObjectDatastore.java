package com.google.code.twig.standard;

import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.conversion.CollectionConverter;
import com.google.code.twig.conversion.CombinedConverter;
import com.google.code.twig.conversion.EngineConverters;
import com.google.code.twig.conversion.CoreConverters;
import com.google.code.twig.conversion.PrimitiveConverter;
import com.google.code.twig.strategy.ActivationStrategy;
import com.google.code.twig.strategy.CombinedStrategy;
import com.google.code.twig.strategy.FieldStrategy;
import com.google.code.twig.strategy.RelationshipStrategy;
import com.google.code.twig.strategy.StorageStrategy;
import com.google.code.twig.translator.ChainedTranslator;
import com.google.code.twig.translator.ConvertableTypeTranslator;
import com.google.code.twig.translator.EnumTranslator;
import com.google.code.twig.translator.NativeDirectTranslator;

public class StandardObjectDatastore extends StrategyObjectDatastore
{
	public StandardObjectDatastore(CombinedStrategy strategy)
	{
		super(strategy);
	}
	
	public StandardObjectDatastore(
			RelationshipStrategy relationshipStrategy,
			StorageStrategy storageStrategy, 
			ActivationStrategy activationStrategy,
			FieldStrategy fieldStrategy)
	{
		super(relationshipStrategy, storageStrategy, activationStrategy, fieldStrategy);
	}
	
	@Override
	protected CombinedConverter createTypeConverter()
	{
		CombinedConverter converter = new CombinedConverter();
		converter.append(new PrimitiveConverter());
		converter.append(new CollectionConverter(converter));
		EngineConverters.registerAll(converter);
		CoreConverters.registerAll(converter);
		return converter;
		
	}

	@Override
	protected ChainedTranslator createValueTranslatorChain()
	{
		ChainedTranslator result = new ChainedTranslator();
		result.append(new NativeDirectTranslator());
		result.append(new ConvertableTypeTranslator(converter));
		result.append(new EnumTranslator());
		return result;
	}

	@Override
	protected PropertyTranslator getFallbackTranslator()
	{
		return getIndependantTranslator();
	}
	
	@Override
	protected boolean isNullStored()
	{
		return true;
	}
}
