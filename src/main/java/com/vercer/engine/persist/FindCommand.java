package com.vercer.engine.persist;

import java.util.Iterator;
import java.util.concurrent.Future;

import com.google.appengine.api.datastore.Cursor;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.SortDirection;
import com.google.common.base.Predicate;


public interface FindCommand
{
	<T> RootFindCommand<T> type(Class<T> type);

	interface BaseFindCommand<C extends BaseFindCommand<C>>
	{
		C filterEntities(Predicate<Entity> filter);
		C filterProperties(Predicate<String> properties);
	}

	interface TypedFindCommand<T, C extends TypedFindCommand<T, C>> extends BaseFindCommand<C>
	{
		C addFilter(String field, FilterOperator operator, Object value);
		BranchFindCommand<T> addChildQuery();
		<P> Future<ParentsCommand<P>> returnParentsCommandLater();
		<P> ParentsCommand<P> returnParentsCommandNow();
		<P> Iterator<P> returnParentsNow();
	}

	interface RootFindCommand<T> extends TypedFindCommand<T, RootFindCommand<T>>
	{
		RootFindCommand<T> addSort(String field);
		RootFindCommand<T> addSort(String field, SortDirection sort);
		RootFindCommand<T> withAncestor(Object ancestor);
		RootFindCommand<T> startFrom(int offset);
		RootFindCommand<T> fetchNoFields();
		RootFindCommand<T> fetchResultsBy(int size);
		RootFindCommand<T> continueFrom(Cursor cursor);

		QueryResultIterator<T> returnResultsNow();
		Future<QueryResultIterator<T>> returnResultsLater();
	}

	interface BranchFindCommand<T> extends TypedFindCommand<T, BranchFindCommand<T>>
	{
		Iterator<T> returnResultsNow();
		Future<Iterator<T>> returnResultsLater();
	}

	interface ParentsCommand<P> extends BaseFindCommand<ParentsCommand<P>>
	{
		Iterator<P> returnParentsNow();
//		Future<Iterator<P>> returnParentsLater();
	}
}
