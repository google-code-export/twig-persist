package com.vercer.util.collections;

import java.util.Map;

public abstract class ReadOnlyAdaptingMap<SK, SV, TK, TV> extends AdaptingMap<SK, SV, TK, TV>
{
	public ReadOnlyAdaptingMap(Map<SK, SV> source)
	{
		super(source);
	}
	
	protected SK unwrapKey(TK target)
	{
		throw new UnsupportedOperationException();
	};
	
	protected SV unwrapValue(TV target)
	{
		throw new UnsupportedOperationException();
	};
}
