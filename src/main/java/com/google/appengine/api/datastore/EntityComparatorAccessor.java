package com.google.appengine.api.datastore;

import java.util.Comparator;
import java.util.List;

import com.google.appengine.api.datastore.PreparedMultiQuery.EntityComparator;
import com.google.appengine.api.datastore.Query.SortPredicate;

public class EntityComparatorAccessor
{
	public static Comparator<Entity> newEntityComparator(List<SortPredicate> sorts)
	{
		return new EntityComparator(sorts);
	}
}
