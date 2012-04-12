package com.vercer.util;

import com.google.common.base.Function;

public class PairFirstFunction<F> implements Function<Pair<F, ?>, F>
{
	@Override
	public F apply(Pair<F, ?> from)
	{
		return from.getFirst();
	}
}
