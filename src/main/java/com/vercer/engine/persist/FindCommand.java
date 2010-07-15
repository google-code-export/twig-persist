package com.vercer.engine.persist;

import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;

public interface FindCommand
{
	enum MergeOperator { OR, AND };
	
	<T> RootFindCommand<T> type(Class<T> type);

	interface BaseFindCommand<C extends BaseFindCommand<C>>
	{
		C restrictEntities(Restriction<Entity> restriction);
		C restrictProperties(Restriction<Property> restriction);
	}

	interface TypedFindCommand<T, C extends TypedFindCommand<T, C>> extends BaseFindCommand<C>
	{
		C addFilter(String field, FilterOperator operator, Object value);
		C addRangeFilter(String field, Object from, Object to);
		BranchFindCommand<T> branch(MergeOperator operator);
	}

	interface BranchFindCommand<T> 
	{
		ChildFindCommand<T> addChildCommand();
	}
	
	interface ChildFindCommand<T> extends TypedFindCommand<T, ChildFindCommand<T>>
	{
	}
	
	/**
	 * @author John Patterson <john@vercer.com>
	 *
	 * @param <T>
	 */
	interface RootFindCommand<T> extends TypedFindCommand<T, RootFindCommand<T>>
	{
		// methods that have side effects
		
		/**
		 * Passed to {@link Query#addSort(String)}
		 * @param field The name of the class field to sort on
		 * @return <code>this</code> for method chaining
		 */
		RootFindCommand<T> addSort(String field);
		
		/**
		 * Passed to {@link Query#addSort(String, SortDirection)}
		 * @param field The name of the class field to sort on
		 * @param sort Direction of sort
		 * @return <code>this</code> for method chaining
		 */
		RootFindCommand<T> addSort(String field, SortDirection sort);
		
		/**
		 * @param ancestor Passed to {@link Query#setAncestor(com.google.appengine.api.datastore.Key)}
		 * @return <code>this</code> for method chaining
		 */
		RootFindCommand<T> ancestor(Object ancestor);
		
		/**
		 * @param offset Set as {@link FetchOptions#offset(int)}
		 * @return <code>this</code> for method chaining
		 */
		RootFindCommand<T> startFrom(int offset);
		
		/**
		 * @param cursor Set as {@link FetchOptions#startCursor(Cursor)}
		 * @return <code>this</code> for method chaining
		 */
		RootFindCommand<T> continueFrom(Cursor cursor);
		
		/**
		 * @param cursor set as {@link FetchOptions#endCursor(Cursor)}
		 * @return <code>this</code> for method chaining
		 */
		RootFindCommand<T> finishAt(Cursor cursor);
		
		
		RootFindCommand<T> maximumResults(int limit);
		RootFindCommand<T> fetchNoFields();
		RootFindCommand<T> fetchNextBy(int size);
		RootFindCommand<T> fetchFirst(int size);

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
	
	interface ParentsCommand<P> extends BaseFindCommand<ParentsCommand<P>>
	{
		Iterator<P> returnParentsNow();
//		Future<Iterator<P>> returnParentsLater();
	}
}
