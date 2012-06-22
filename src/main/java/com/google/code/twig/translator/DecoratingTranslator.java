package com.google.code.twig.translator;

import com.google.code.twig.PropertyTranslator;

public abstract class DecoratingTranslator implements PropertyTranslator
{
	protected final PropertyTranslator delegate;

	public DecoratingTranslator(PropertyTranslator chained)
	{
		this.delegate = chained;
	}
}