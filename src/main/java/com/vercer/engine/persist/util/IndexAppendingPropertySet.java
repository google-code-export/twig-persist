/**
 * 
 */
package com.vercer.engine.persist.util;

import java.util.Set;

import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.util.collections.ReadOnlyAdaptingSet;

public final class IndexAppendingPropertySet extends ReadOnlyAdaptingSet<Property, Property>
{

	private final int index;

	public IndexAppendingPropertySet(Set<Property> source, int index)
	{
		super(source);
		this.index = index;
	}

	@Override
	protected Property wrap(final Property source)
	{
		return new Property()
		{
			public Path getPath()
			{
				return new Path.Builder(source.getPath()).array(index).build();
			}

			public Object getValue()
			{
				return source.getValue();
			}
			
			public boolean isIndexed()
			{
				return source.isIndexed();
			}
		};
	}
}