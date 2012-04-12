package com.vercer.util.collections;

import java.util.Map;


public abstract class ValueAdapterMap<K, TargetV, SourceV> extends AdaptingMap<K, SourceV, K, TargetV>
{
	public ValueAdapterMap(Map<K, SourceV> source)
	{
		super(source);
	}
	
	@Override
	protected K wrapKey(K source)
	{
		return source;
	};
	
	protected K unwrapKey(K target)
	{ 
		return target; 
	};
}
