package com.vercer.util.collections;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;


public abstract class AdaptingMap<SourceK, SourceV, TargetK, TargetV> extends AbstractMap<TargetK, TargetV>
{
	private final Map<SourceK, SourceV> source;

	public AdaptingMap(Map<SourceK, SourceV> source)
	{
		this.source = source;
	}

	@Override
	public Set<Entry<TargetK, TargetV>> entrySet()
	{
		Set<Entry<SourceK,SourceV>> entrySet = source.entrySet();
		return new ReadOnlyAdaptingSet<Entry<TargetK,TargetV>, Entry<SourceK,SourceV>>(entrySet)
		{
			@Override
			protected Entry<TargetK, TargetV> wrap(final Entry<SourceK, SourceV> source)
			{
				return new Entry<TargetK, TargetV>()
				{
					public TargetK getKey()
					{
						return wrapKey(source.getKey());
					}

					public TargetV getValue()
					{
						return wrapValue(source.getValue());
					}

					public TargetV setValue(TargetV value)
					{
						return wrapValue(source.setValue(unwrapValue(value)));
					}
				};
			}
		};
	}

	public TargetV put(TargetK key, TargetV value)
	{
		return wrapValue(source.put(unwrapKey(key), unwrapValue(value)));
	};
	
	@SuppressWarnings("unchecked")
	@Override
	public TargetV get(Object key)
	{
		return wrapValue(source.get(unwrapKey((TargetK) key)));
	}
	
	protected abstract TargetV wrapValue(SourceV source);
	protected abstract TargetK wrapKey(SourceK source);
	protected abstract SourceV unwrapValue(TargetV target);
	protected abstract SourceK unwrapKey(TargetK target);
	
}
