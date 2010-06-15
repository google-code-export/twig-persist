package com.vercer.engine.persist;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.DatastoreServiceConfig;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.QueryResultList;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.base.Predicate;


public interface FindCommand
{
	enum MergeOperator { OR, AND };
	
	<T> RootFindCommand<T> type(Class<T> type);

	interface BaseFindCommand<C extends BaseFindCommand<C>>
	{
		C filterEntities(Predicate<Entity> filter);
		C filterProperties(Predicate<Property> filter);
	}

	interface TypedFindCommand<T, C extends TypedFindCommand<T, C>> extends BaseFindCommand<C>
	{
		C addFilter(String field, FilterOperator operator, Object value);
		C addRangeFilter(String field, Object from, Object to);
		SplitFindCommand<T> split(MergeOperator operator);
	}

	interface SplitFindCommand<T> 
	{
		BranchFindCommand<T> addBranch();
	}
	
	interface RootFindCommand<T> extends TypedFindCommand<T, RootFindCommand<T>>
	{
		// methods that have side effects
		RootFindCommand<T> addSort(String field);
		RootFindCommand<T> addSort(String field, SortDirection sort);
		RootFindCommand<T> ancestor(Object ancestor);
		RootFindCommand<T> startFrom(int offset);
		RootFindCommand<T> fetchNoFields();
		RootFindCommand<T> fetchResultsBy(int size);
		RootFindCommand<T> continueFrom(Cursor cursor);

		// terminating methods
		int countResultsNow();

		QueryResultIterator<T> returnResultsNow();
		List<T> returnAllResultsNow();
		
		Future<QueryResultIterator<T>> returnResultsLater();
		Future<? extends List<T>> returnAllResultsLater();
		
		<P> Iterator<P> returnParentsNow();
		<P> ParentsCommand<P> returnParentsCommandNow();
		<P> Future<ParentsCommand<P>> returnParentsCommandLater();
	}
	
	interface BranchFindCommand<T> extends TypedFindCommand<T, BranchFindCommand<T>>
	{
	}

	interface ParentsCommand<P> extends BaseFindCommand<ParentsCommand<P>>
	{
		Iterator<P> returnParentsNow();
//		Future<Iterator<P>> returnParentsLater();
	}
}
