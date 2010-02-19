package com.vercer.engine.persist;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.com.google.common.collect.Iterables;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ForwardingIterator;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.vercer.engine.persist.TypesafeDatastore.FindOptions;
import com.vercer.engine.persist.util.PropertyMapToSet;
import com.vercer.engine.persist.util.SortedMergeIterator;
import com.vercer.engine.persist.util.generic.GenericTypeReflector;
import com.vercer.engine.util.Entities;
import com.vercer.util.reference.ObjectReference;

/**
 * Stateless base responsible for core datastore functionality that does not
 * depend on an instances identity. Encoding and decoding instances and entities 
 * is performed by the PropertyTranslator.
 *  
 * @author John Patterson <john@vercer.com>
 */
public abstract class TranslatorTypesafeDatastore implements TypesafeDatastore
{
	private final DatastoreService service;
	private PropertyTranslator translator;
	private boolean indexed;
	
	private Function<Entity, Object> defaultEntityToInstanceFunction = new EntityToInstanceFunction<Object>(null);
	private Function<Entity, Object> defaultKeyToInstanceFunction = new KeyToInstanceFunction<Object>(null);
	private Function<Entity, Object> defaultParentKeyToInstanceFunction = new ParentKeyToInstanceFunction<Object>(null);

	public TranslatorTypesafeDatastore(DatastoreService service, PropertyTranslator translator, boolean indexed)
	{
		this.service = service;
		this.translator = translator;
		this.indexed = indexed;
	}

	protected TranslatorTypesafeDatastore(DatastoreService datastore)
	{
		this.service = datastore;
	}

	protected void setPropertyTranslator(PropertyTranslator translator)
	{
		this.translator = translator;
	}

	protected void setIndexed(boolean indexed)
	{
		this.indexed = indexed;
	}

	protected final Key store(Object instance, Key parentKey, String name)
	{
		onBeforeStore(instance);

		Entity entity;
		try
		{
			Set<Property> properties = translator.typesafeToProperties(instance, Path.EMPTY_PATH, indexed);
			if (properties == null)
			{
				throw new IllegalStateException("Could not translate instance: " + instance);
			}

			String kind = typeToKind(instance.getClass());

			entity = createEntity(new KeySpecification(kind, parentKey, name));

			transferProperties(entity, properties);
		}
		catch (Exception e)
		{
			throw newExceptionOnTranslateWrite(e, instance);
		}

		Key key = putEntityToDatstore(entity);

		onAfterStore(instance, entity);

		return key;
	}

	private void transferProperties(Entity entity, Collection<Property> properties)
	{
		for (Property property : properties)
		{
			// dereference object references
			Object value = property.getValue();
			if (value instanceof ObjectReference<?>)
			{
				value = ((ObjectReference<?>)value).get();
			}
			else if (value instanceof List<?>)
			{
				// we know the value is a mutable list from CollectionTranslator
				@SuppressWarnings("unchecked")
				List<Object> values = (List<Object>) value;
				for (int i = 0; i < values.size(); i++)
				{
					Object item = values.get(i);
					if (item instanceof ObjectReference<?>)
					{
						// dereference the value and set it in-place
						Object dereferenced = ((ObjectReference<?>) item).get();
						values.set(i, dereferenced);
					}
					else
					{
						// assume that they are all references or none
						break;
					}
				}
			}

			if (property.isIndexed())
			{
				entity.setProperty(property.getPath().toString(), value);
			}
			else
			{
				entity.setUnindexedProperty(property.getPath().toString(), value);
			}
		}
	}

	/**
	 * Give subclasses a chance to intercept all datastore puts
	 */
	protected Key putEntityToDatstore(Entity entity)
	{
		return service.put(entity);
	}

	public final Key store(Object instance, String name)
	{
		return store(instance, name, null);
	}

	protected final Key store(Object value, Key parentKey)
	{
		return store(value, parentKey, null);
	}

	public final Key store(Object instance)
	{
		return store(instance, (Key) null, null);
	}

	public Object encode(Object object)
	{
		Set<Property> properties = translator.typesafeToProperties(object, Path.EMPTY_PATH, true);
		return properties.iterator().next().getValue();
	}

	protected Entity createEntity(KeySpecification spec)
	{
		Key parent = null;
		if (spec.getParentKeyReference() != null)
		{
			parent = spec.getParentKeyReference().get();
		}
		return Entities.createEntity(spec.getKind(), spec.getName(), parent);
	}

	/**
	 * Override to provide shorter kind names reducing the key size
	 * @param clazz The class to store
	 * @return A shortened kind name
	 */
	protected String typeToKind(Type type)
	{
		Class<?> clazz = GenericTypeReflector.erase(type);
		return clazz.getName();
	}

	protected Type kindToType(String kind)
	{
		try
		{
			return Class.forName(kind);
		}
		catch (ClassNotFoundException e)
		{
			throw new IllegalStateException(e);
		}
	}

	public final <T> T load(Class<T> type, Object key)
	{
		return load(type, key, null);
	}

	protected final <T> T internalLoad(Class<T> type, String name, Key parent)
	{
		String kind = typeToKind(type);
		
		Key key;
		if (parent == null)
		{
			key = KeyFactory.createKey(kind, name);
		}
		else
		{
			key = KeyFactory.createKey(parent, kind, name);
		}

		// needed to avoid sun generics bug "no unique maximal instance exists..."
		@SuppressWarnings("unchecked")
		T result = (T) load(key);
		return result;
	}

	protected final <T> Iterator<T> find(Class<T> type, Key parent, FindOptions options)
	{
		String kind = typeToKind(type);
		Query query = new Query(kind, parent);
		return find(query, options);
	}

	public final <T> Iterator<T> find(Class<T> type)
	{
		return find(type, (FindOptions) null);
	}

	public final <T> Iterator<T> find(Class<T> type, FindOptions options)
	{
		String kind = typeToKind(type);
		Query query = new Query(kind);
		return find(query, options);
	}

	public final <T> T toTypesafe(Entity entity)
	{
		@SuppressWarnings("unchecked") // needed for sun javac bug
		T result = (T) toTypesafe(entity, null);
		return result;
	}
	
	public <T> T toTypesafe(Entity entity, Predicate<String> predicate)
	{
		onBeforeRestore(entity);

		Type type = kindToType(entity.getKind());

		// filter out unwanted properties at the lowest level
		Map<String, Object> basic = entity.getProperties();
		if (predicate != null)
		{
			basic = Maps.filterKeys(basic, predicate);
		}
		
		Set<Property> properties = new PropertyMapToSet(basic, indexed);

		// order the properties for efficient separation by field
		properties = new TreeSet<Property>(properties);

		@SuppressWarnings("unchecked")
		T result = (T) translator.propertiesToTypesafe(properties, Path.EMPTY_PATH, type);
		if (result == null)
		{
			throw new IllegalStateException("Could not translate entity " + entity);
		}

		onAfterRestore(entity, result);

		return result;
	}

	protected void onAfterRestore(Entity entity, Object instance)
	{
	}

	protected void onBeforeRestore(Entity entity)
	{
	}

	public Query query(Class<?> type)
	{
		return new Query(typeToKind(type));
	}

	protected RuntimeException newExceptionOnTranslateWrite(Exception e, Object instance)
	{
		return new RuntimeException(e);
	}

	protected void onBeforeStore(Object instance)
	{
	}

	protected void onAfterStore(Object instance, Entity entity)
	{
	}

	public final <T> T load(Key key)
	{
		@SuppressWarnings("unchecked")
		T result = (T) keyToInstance(key, null);
		return result;
	}
	
	@SuppressWarnings("unchecked")
	protected <T> T keyToInstance(Key key, Predicate<String> propertyPredicate)
	{
		Entity entity = keyToEntity(key);
		if (entity == null)
		{
			return null;
		}
		else
		{
			return (T) toTypesafe(entity, propertyPredicate);
		}
	}

	protected Entity keyToEntity(Key key)
	{
		try
		{
			return service.get(key);
		}
		catch (EntityNotFoundException e)
		{
			return null;
		}
	}

	public final DatastoreService getService()
	{
		return service;
	}

	public final <T> Iterator<T> find(Query query)
	{
		return find(query, null);
	}

	public final <T> Iterator<T> find(Query query, FindOptions options)
	{
		Iterator<Entity> iterator = queryToEntityIterator(query, options);
		Function<Entity, T> function = entityToInstanceFunction(query, options);
		return (Iterator<T>) Iterators.transform(iterator, function);
	}

	@SuppressWarnings("unchecked")
	private <T> Function<Entity, T> entityToInstanceFunction(Query query, FindOptions options)
	{
		Function<Entity, T> function;
//		if (query.isKeysOnly())
//		{
//			if (options.isReturnParent())
//			{
//				function = (Function<Entity, T>) defaultParentKeyToInstanceFunction;
//				if (options != null && options.getPropertyPredicate() != null)
//				{
//					function = new ParentKeyToInstanceFunction<T>(options.getPropertyPredicate());
//				}
//			}
//			else
//			{
//				function = (Function<Entity, T>) defaultKeyToInstanceFunction;
//				if (options != null && options.getPropertyPredicate() != null)
//				{
//					function = new KeyToInstanceFunction<T>(options.getPropertyPredicate());
//				}
//			}
//		}
//		else
//		{
			function = (Function<Entity, T>) defaultEntityToInstanceFunction;
			if (options != null && options.getPropertyPredicate() != null)
			{
				function = new EntityToInstanceFunction<T>(options.getPropertyPredicate());
			}
//		}
		return function;
	}
	
	public <T> Iterator<T> find(Collection<Query> queries, MergeFindOptions options)
	{
		if (options.getInstanceComparator() != null || options.getEntityComparator() == null)
		{
			throw new UnsupportedOperationException("Not implemented yet");
		}
		
		List<Iterator<Entity>> iterators = new ArrayList<Iterator<Entity>>();
		for (Query query : queries)
		{
			Iterator<Entity> iterator = queryToEntityIterator(query, options);
			iterators.add(iterator);
		}
		
		// use the first query to decide what function to create
		Function<Entity, T> function = entityToInstanceFunction(queries.iterator().next(), options);
		
		// merge all entities into a single stream
		SortedMergeIterator<Entity> merged = 
			new SortedMergeIterator<Entity>(
					options.getEntityComparator(), 
					iterators, 
					options.isFilterDuplicates());
		
		// convert the entity stream into an instance stream
		return (Iterator<T>) Iterators.transform(merged, function);
	}

	public final void update(Object instance, Key key)
	{
		// TODO share more code with store()
		Entity entity = new Entity(key);
		try
		{
			Set<Property> properties = translator.typesafeToProperties(instance, Path.EMPTY_PATH, indexed);
			if (properties == null)
			{
				throw new IllegalStateException("Could not translate instance: " + instance);
			}

			// clear all existing properties
			Set<String> names = entity.getProperties().keySet();
			for (String name : names)
			{
				entity.removeProperty(name);
			}

			// copy in new properties
			transferProperties(entity, properties);
		}
		catch (Exception e)
		{
			throw newExceptionOnTranslateWrite(e, instance);
		}

		putEntityToDatstore(entity);

		onAfterUpdate(instance, entity);
	}

	protected void onAfterUpdate(Object instance, Entity entity)
	{
	}

	public final void deleteAll(Class<?> type)
	{
		Query query = query(type);
		query.setKeysOnly();
		FetchOptions options = FetchOptions.Builder.withChunkSize(100);
		Iterator<Entity> entities = service.prepare(query).asIterator(options);
		Iterator<Key> keys = Iterators.transform(entities, entityToKeyFunction);
		Iterator<List<Key>> partitioned = Iterators.partition(keys, 100);
		while (partitioned.hasNext())
		{
			deleteKeys(partitioned.next());
		}
	}
	
	protected final void deleteKeys(Collection<Key> keys)
	{
		service.delete(keys);
		onAfterDelete(keys);
	}

	protected void onAfterDelete(Collection<Key> keys)
	{
	}

	private static final Function<Entity, Key> entityToKeyFunction = new Function<Entity, Key>()
	{
		public Key apply(Entity arg0)
		{
			return arg0.getKey();
		}
	};
	
	protected Iterator<Entity> queryToEntityIterator(Query query, final FindOptions options)
	{
		Iterator<Entity> result;
		if (options != null && options.getFetchOptions() != null)
		{
			result = service.prepare(query).asIterator(options.getFetchOptions());
		}
		else 
		{
			result = service.prepare(query).asIterator();
		}
		
		if (options != null && options.isReturnParent())
		{
			result = new PreFetchParentIterator(options, result);
		}
		
		if (options != null && options.getEntityPredicate() != null)
		{
			result = Iterators.filter(result, options.getEntityPredicate());
		}
		
		return result;
	}

	private final class PreFetchParentIterator extends AbstractIterator<Entity>
	{
		private final FindOptions options;
		private final Iterator<Entity> result;
		private Iterator<Entity> parents;

		private PreFetchParentIterator(FindOptions options, Iterator<Entity> result)
		{
			this.options = options;
			this.result = result;
		}

		@Override
		protected Entity computeNext()
		{
			if (parents == null)
			{
				if (!result.hasNext())
				{
					return endOfData();
				}
				
				// match the key iterator chunk size
				List<Key> keys = new ArrayList<Key>(options.getFetchOptions().getChunkSize());
				for (int i = 0; result.hasNext() && i < options.getFetchOptions().getChunkSize(); i++)
				{
					keys.add(result.next().getKey().getParent());
				}
				
				Map<Key, Entity> map = service.get(keys);
				
				parents = map.values().iterator();
				if (parents.hasNext() == false)
				{
					return endOfData();
				}
			}

			if (parents.hasNext())
			{
				return parents.next();
			}
			else
			{
				parents = null;
				return computeNext();
			}
		}
	}

	private final class EntityToInstanceFunction<T> implements Function<Entity, T>
	{
		private final Predicate<String> predicate;

		public EntityToInstanceFunction(Predicate<String> predicate)
		{
			this.predicate = predicate;	
		}

		@SuppressWarnings("unchecked")
		public T apply(Entity entity)
		{
			return (T) toTypesafe(entity, predicate);
		}
	}

	private final class KeyToInstanceFunction<T> implements Function<Entity, T>
	{
		private final Predicate<String> propertyPredicate;
		
		public KeyToInstanceFunction(Predicate<String> propertyPredicate)
		{
			this.propertyPredicate = propertyPredicate;
		}

		public T apply(Entity entity)
		{
			@SuppressWarnings("unchecked")
			T result = (T) keyToInstance(entity.getKey(), propertyPredicate);
			return result;
		}
	}
	
	private final class ParentKeyToInstanceFunction<T> implements Function<Entity, T>
	{
		private final Predicate<String> propertyPredicate;
		
		public ParentKeyToInstanceFunction(Predicate<String> propertyPredicate)
		{
			this.propertyPredicate = propertyPredicate;
		}

		public T apply(Entity entity)
		{
			@SuppressWarnings("unchecked")
			T result = (T) keyToInstance(entity.getKey().getParent(), propertyPredicate);
			return result;
		}
	}
}
