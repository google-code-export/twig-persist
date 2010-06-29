package com.vercer.engine.persist.translator;

import com.vercer.engine.persist.PropertyTranslator;

public abstract class DecoratingTranslator implements PropertyTranslator
{
	protected final PropertyTranslator chained;

	public DecoratingTranslator(PropertyTranslator chained)
	{
		this.chained = chained;
	}
}