package com.vercer.engine.persist.standard;

import java.io.NotSerializableException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vercer.engine.persist.FindCommand;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.StoreCommand;
import com.vercer.engine.persist.conversion.CombinedTypeConverter;
import com.vercer.engine.persist.conversion.DefaultTypeConverter;
import com.vercer.engine.persist.conversion.TypeConverter;
import com.vercer.engine.persist.strategy.ActivationStrategy;
import com.vercer.engine.persist.strategy.CacheStrategy;
import com.vercer.engine.persist.strategy.CombinedStrategy;
import com.vercer.engine.persist.strategy.FieldStrategy;
import com.vercer.engine.persist.strategy.RelationshipStrategy;
import com.vercer.engine.persist.strategy.StorageStrategy;
import com.vercer.engine.persist.translator.ChainedTranslator;
import com.vercer.engine.persist.translator.CoreStringTypesTranslator;
import com.vercer.engine.persist.translator.EnumTranslator;
import com.vercer.engine.persist.translator.ListTranslator;
import com.vercer.engine.persist.translator.MapTranslator;
import com.vercer.engine.persist.translator.NativeDirectTranslator;
import com.vercer.engine.persist.translator.ObjectFieldTranslator;
import com.vercer.engine.persist.translator.PolymorphicTranslator;
import com.vercer.engine.persist.util.Entities;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.util.reference.ObjectReference;

/**
 * Stateful layer responsible for caching key-object references and
 * creating a PropertyTranslator that can be configured using Strategy
 * instances.
 *
 * @author John Patterson <john@vercer.com>
 */
public class StrategyObjectDatastore extends BaseObjectDatastore
{
	KeySpecification encodeKeySpec;
	Key decodeKey;
	
	// activation depth cannot be in decode context because it is defined per field
	private Deque<Integer> activationDepthDeque= new ArrayDeque<Integer>();
	private boolean indexed;

	private final PropertyTranslator objectFieldTranslator;
	private final PropertyTranslator embedTranslator;
	private final PropertyTranslator polyMorphicComponentTranslator;
	private final PropertyTranslator parentTranslator;
	private final PropertyTranslator independantTranslator;
	private final PropertyTranslator keyFieldTranslator;
	private final PropertyTranslator childTranslator;
	private final ChainedTranslator valueTranslatorChain;
	private final PropertyTranslator defaultTranslator;

	// TODO refactor this into an InstanceStrategy
	private final KeyCache keyCache;

	/**
	 * Flag that indicates we are associating instances with this session so do not store them
	 */
	// TODO store key field to do this - remove this flag
	private boolean associating;
	private Object refresh;
	
	private Map<Object, Entity> batched;

	private TypeConverter converter;

	// TODO make all these private when commands have no logic
	final RelationshipStrategy relationshipStrategy;
	final FieldStrategy fieldStrategy;
	final ActivationStrategy activationStrategy;
	final StorageStrategy storageStrategy;
	final CacheStrategy cacheStrategy;

	public StrategyObjectDatastore(DatastoreService service, CombinedStrategy strategy)
	{
		this(service, strategy, strategy, strategy, strategy, strategy);
	}

	public StrategyObjectDatastore(DatastoreService datastore,
			RelationshipStrategy relationshipStrategy,
			StorageStrategy storageStrategy,
			CacheStrategy cacheStrategy,
			ActivationStrategy activationStrategy,
			FieldStrategy fieldStrategy)
	{
		// use the protected constructor so we can configure the translator
		super(datastore);

		// push the default depth onto the stack
		activationDepthDeque.push(Integer.MAX_VALUE);
		
		this.activationStrategy = activationStrategy;
		this.cacheStrategy = cacheStrategy;
		this.fieldStrategy = fieldStrategy;
		this.relationshipStrategy = relationshipStrategy;
		this.storageStrategy = storageStrategy;

		converter = createTypeConverter();

		objectFieldTranslator = new StrategyObjectFieldTranslator(converter);

		valueTranslatorChain = createValueTranslatorChain();

		parentTranslator = new ParentEntityTranslator(this);
		independantTranslator = new EntityTranslator(this);
		keyFieldTranslator = new KeyFieldTranslator(this, valueTranslatorChain, converter);
		childTranslator = new ChildEntityTranslator(this);
		embedTranslator = new ListTranslator(objectFieldTranslator);
		polyMorphicComponentTranslator = new ListTranslator(new MapTranslator(new PolymorphicTranslator(objectFieldTranslator, fieldStrategy), converter));
		defaultTranslator = new ListTranslator(new MapTranslator(new ChainedTranslator(valueTranslatorChain, getFallbackTranslator()), converter));

		keyCache = new KeyCache();
	}
	
	protected PropertyTranslator decoder(Entity entity)
	{
		return objectFieldTranslator;
	}

	protected PropertyTranslator encoder(Object instance)
	{
		return objectFieldTranslator;
	}
	
	protected PropertyTranslator decoder(Field field, Set<Property> properties)
	{
		return translator(field);
	}

	protected PropertyTranslator encoder(Field field, Object instance)
	{
		return translator(field);
	}

	protected PropertyTranslator translator(Field field)
	{
		if (storageStrategy.entity(field))
		{
			PropertyTranslator translator;
			if (relationshipStrategy.parent(field))
			{
				translator = parentTranslator;
			}
			else if (relationshipStrategy.child(field))
			{
				translator = childTranslator;
			}
			else
			{
				translator = independantTranslator;
			}
			
//			if (cacheStrategy.cache(field))
//			{
//				
//			}
			
			return translator;
		}
		else if (relationshipStrategy.key(field))
		{
			return keyFieldTranslator;
		}
		else if (storageStrategy.embed(field))
		{
			if (storageStrategy.polymorphic(field))
			{
				return polyMorphicComponentTranslator;
			}
			else
			{
				return embedTranslator;
			}
		}
		else
		{
			return defaultTranslator;
		}
	}

	/**
	 * @return The translator which is used if no others are configured
	 */
	protected PropertyTranslator getFallbackTranslator()
	{
		return getIndependantTranslator();
	}

	protected TypeConverter createTypeConverter()
	{
		return new DefaultTypeConverter();
	}

	/**
	 * @return The translator that is used for single items by default
	 */
	protected ChainedTranslator createValueTranslatorChain()
	{
		ChainedTranslator result = new ChainedTranslator();
		result.append(new NativeDirectTranslator());
		result.append(new CoreStringTypesTranslator());
		result.append(new EnumTranslator());
		return result;
	}

	/**
	 * Potentially store an entity in the datastore.
	 */
	protected Key putEntity(Entity entity)
	{
		// we could be just pretending to store to process the instance to get its key
		if (associating)
		{
			// do not save the entity because we just want the key
			Key key = entity.getKey();
			if (!key.isComplete())
			{
				// incomplete keys are no good to us
				throw new IllegalArgumentException("Associating entity does not have complete key: " + entity);
			}
			return key;
		}
		else if (batched != null)
		{
			// don't store anything yet if we are batching writes
			Key key = entity.getKey();
			
			// referenced entities must have a full key without being stored
			if (!encodeKeySpec.isComplete())
			{
				// incomplete keys are no good to us - we need a key now
				throw new IllegalArgumentException("Must have complete key in batched mode for entity " + entity);
			}

			// we will get the key after put
			return key;
		}
		else
		{
			// actually put the entity in the datastore
			return servicePut(entity);
		}
	}

	@SuppressWarnings("unchecked")
	final <T> T entityToInstance(Entity entity, Predicate<Property> filter)
	{
		T instance = (T) keyCache.getInstance(entity.getKey());
		if (instance == null)
		{
			// push a new context
			Key existingDecodeKey = decodeKey;
			decodeKey = entity.getKey();

			Type type = fieldStrategy.kindToType(entity.getKind());

			Set<Property> properties = PropertySets.create(entity.getProperties(), indexed);
			
			// filter out unwanted properties at the lowest level
			if (filter != null)
			{
				properties = Sets.filter(properties, filter);
			}

			// order the properties for efficient separation by field
			properties = new TreeSet<Property>(properties);

			instance = (T) decoder(entity).propertiesToTypesafe(properties, Path.EMPTY_PATH, type);
			if (instance == null)
			{
				throw new IllegalStateException("Could not translate entity " + entity);
			}

			// pop the context
			decodeKey = existingDecodeKey;
		}

		return instance;
	}
	

	final <T> Iterator<T> entitiesToInstances(final Iterator<Entity> entities, final Predicate<Property> filter)
	{
		return new Iterator<T>()
		{
			@Override
			public boolean hasNext()
			{
				return entities.hasNext();
			}

			@Override
			public T next()
			{
				return entityToInstance(entities.next(), filter);
			}

			@Override
			public void remove()
			{
				entities.remove();
			}
		};
	}


	@SuppressWarnings("unchecked")
	<T> T keyToInstance(Key key, Predicate<Property> filter)
	{
		T instance = (T) keyCache.getInstance(key);
		if (instance == null)
		{
			Entity entity = keyToEntity(key);
			if (entity == null)
			{
				instance = null;
			}
			else
			{
				instance = (T) entityToInstance(entity, filter);
			}
		}

		return instance;
	}
	
	@SuppressWarnings("unchecked")
	final <T> Map<Key, T> keysToInstances(List<Key> keys, Predicate<Property> filter)
	{
		Map<Key, T> result = new HashMap<Key, T>(keys.size());
		List<Key> missing = null;
		for (Key key : keys)
		{
			T instance = (T) keyCache.getInstance(key);
			if (instance != null)
			{
				result.put(key, instance);
			}
			else
			{
				if (missing == null)
				{
					missing = new ArrayList<Key>(keys.size());
				}
				missing.add(key);
			}
		}
		
		if (!missing.isEmpty())
		{
			Map<Key, Entity> entities = keysToEntities(missing);
			if (!entities.isEmpty())
			{
				Set<Entry<Key, Entity>> entries = entities.entrySet();
				for (Entry<Key, Entity> entry : entries)
				{
					T instance = entityToInstance(entry.getValue(), filter);
					result.put(entry.getKey(), instance);
				}
			}
		}

		return result;
	}

	final Entity keyToEntity(Key key)
	{
		if (getActivationDepth() > 0)
		{
			try
			{
				return serviceGet(key);
			}
			catch (EntityNotFoundException e)
			{
				return null;
			}
		}
		else
		{
			// don't load entity if it will not be activated
			return new Entity(key);
		}
	}
	
	final Map<Key, Entity> keysToEntities(Collection<Key> keys)
	{
		// only load entity if we will activate instance
		if (getActivationDepth() > 0)
		{
			return serviceGet(keys);
		}
		else
		{
			// we must return empty entities with the correct kind to instantiate
			HashMap<Key, Entity> result = new HashMap<Key, Entity>();
			for (Key key : keys)
			{
				result.put(key, new Entity(key));
			}
			return result;
		}
	}
	
	// TODO make almost every method private once commands contain no logic
	final Entity createEntity()
	{
		if (encodeKeySpec.isComplete())
		{
			// we have a complete key with id specified 
			return new Entity(encodeKeySpec.toKey());
		}
		else
		{
			// we have no id specified so must create entity for auto-generated id
			ObjectReference<Key> parentKeyReference = encodeKeySpec.getParentKeyReference();
			Key parentKey = parentKeyReference == null ? null : parentKeyReference.get();
			return Entities.createEntity(encodeKeySpec.getKind(), null, parentKey);
		}
	}

	protected boolean propertiesIndexedByDefault()
	{
		return true;
	}

	@Override
	public final void disassociate(Object reference)
	{
		keyCache.evictInstance(reference);
	}

	@Override
	public final void disassociateAll()
	{
		keyCache.clear();
	}

	@Override
	public final void associate(Object instance, Key key)
	{
		keyCache.cache(key, instance);
	}

	@Override
	public final void associate(Object instance)
	{
		// convert the instance so we can get its key and children
		associating = true;
		store(instance);
		associating = false;
	}

	@Override
	public final Key associatedKey(Object instance)
	{
		return keyCache.getKey(instance);
	}

	@Override
	public final <T> T load(Class<T> type, Object id, Object parent)
	{
		Object converted;
		if (Number.class.isAssignableFrom(id.getClass()))
		{
			converted = converter.convert(id, Long.class);
		}
		else
		{
			converted = converter.convert(id, String.class);
		}

		Key parentKey = null;
		if (parent != null)
		{
			parentKey = keyCache.getKey(parent);
		}
		return internalLoad(type, converted, parentKey);
	}

	@Override
	public final <I, T> Map<I, T>  loadAll(Class<? extends T> type, Collection<I> ids)
	{
		Map<I, T> result = new HashMap<I, T>(ids.size()); 
		for (I id : ids)
		{
			// TODO optimise this
			T loaded = load(type, id);
			result.put(id, loaded);
		}

		return result;
	}

	@Override
	public final void update(Object instance)
	{
		Key key = keyCache.getKey(instance);
		if (key == null)
		{
			throw new IllegalArgumentException("Can only update instances loaded from this session");
		}
		internalUpdate(instance, key);
	}

	@Override
	public final void storeOrUpdate(Object instance)
	{
		if (associatedKey(instance) != null)
		{
			update(instance);
		}
		else
		{
			store(instance);
		}
	}

	@Override
	public final void storeOrUpdate(Object instance, Object parent)
	{
		if (associatedKey(instance) != null)
		{
			update(instance);
		}
		else
		{
			store(instance, parent);
		}
	}

	@Override
	public final void delete(Object instance)
	{
		Key key= keyCache.getKey(instance);
		if (key == null)
		{
			throw new IllegalArgumentException("Instance " + instance + " is not associated");
		}
		deleteKeys(Collections.singleton(key));
	}

	@Override
	public final void deleteAll(Collection<?> instances)
	{
		deleteKeys(Collections2.transform(instances, cachedInstanceToKeyFunction));
	}

	/**
	 * Either gets exiting key from cache or first stores the instance then returns the key
	 */
	Key instanceToKey(Object instance, Key parentKey)
	{
		Key key = keyCache.getKey(instance);
		if (key == null)
		{
			key = internalStore(instance, parentKey, null);
		}
		return key;
	}
	
	<T> Map<T, Key> instancesToKeys(Collection<T> instances, Object parent)
	{
		Map<T, Key> result = new HashMap<T, Key>(instances.size());
		List<T> missed = new ArrayList<T>(instances.size());
		for (T instance : instances)
		{
			Key key = keyCache.getKey(instance);
			if (key == null)
			{
				missed.add(instance);
			}
			else
			{
				result.put(instance, key);
			}
		}
		
		if (!missed.isEmpty())
		{
			result.putAll(storeAll(missed, parent));
		}
		
		return result;
	}

	@Override
	public final Key store(Object instance, Object parent)
	{
		return store(instance, parent, null);
	}

	@Override
	public final Key store(Object instance, Object parent, String name)
	{
		Key parentKey = null;
		if (parent != null)
		{
			parentKey= keyCache.getKey(parent);
		}
		
		return internalStore(instance, parentKey, name);
	}

	final Key internalStore(Object instance, Key parentKey, String name)
	{
		// cache the empty key details now in case a child references back to us
		if (keyCache.getKey(instance) != null)
		{
			throw new IllegalStateException("Cannot store same instance twice: " + instance);
		}
		Entity entity = instanceToEntity(instance, parentKey, name);
		Key key = putEntity(entity);
		
		// replace the temp key ObjRef with the full key for this instance 
		keyCache.cache(key, instance);
		return key;
	}
	
	@Override
	public final <T> Map<T, Key> storeAll(Collection<? extends T> instances)
	{
		return storeAll(instances, (Key) null);
	}

	@Override
	public final <T> Map<T, Key> storeAll(Collection<? extends T> instances, Object parent)
	{
		// encode the instances to entities
		final Map<T, Entity> entities = instancesToEntities(instances, parent, false);
		
		// actually put them in the datastore and get their keys
		final List<Key> keys = servicePut(entities.values());
		
		LinkedHashMap<T, Key> result = Maps.newLinkedHashMap();
		Iterator<T> instanceItr = entities.keySet().iterator();
		Iterator<Key> keyItr = keys.iterator();
		while (instanceItr.hasNext())
		{
			// iterate instances and keys in parallel
			T instance = instanceItr.next();
			Key key = keyItr.next();
			
			// replace the temp key ObjRef with the full key for this instance 
			keyCache.cache(key, instance);
			
			result.put(instance, key);
		}
		return result;
	}

	@Override
	public final void refresh(Object instance)
	{
		Key key = associatedKey(instance);

		if (key == null)
		{
			throw new IllegalStateException("Instance not associated with session");
		}

		// so it is not loaded from the cache
		disassociate(instance);

		// load will use this instance instead of creating new
		refresh = instance;

		// load from datastore into the refresh instance
		Object loaded = load(key);

		if (loaded == null)
		{
			throw new IllegalStateException("Instance to be refreshed could not be found");
		}
	}

	@Override
	public final int getActivationDepth()
	{
		return activationDepthDeque.peek();
	}

	@Override
	public final void setActivationDepth(int depth)
	{
		activationDepthDeque.pop();
		activationDepthDeque.push(depth);
	}

	protected final PropertyTranslator getIndependantTranslator()
	{
		return independantTranslator;
	}

	protected final PropertyTranslator getChildTranslator()
	{
		return childTranslator;
	}

	protected final PropertyTranslator getParentTranslator()
	{
		return parentTranslator;
	}

	protected final PropertyTranslator getPolyMorphicComponentTranslator()
	{
		return polyMorphicComponentTranslator;
	}

	protected final PropertyTranslator getEmbedTranslator()
	{
		return embedTranslator;
	}

	protected final PropertyTranslator getKeyFieldTranslator()
	{
		return keyFieldTranslator;
	}

	protected final PropertyTranslator getDefaultTranslator()
	{
		return defaultTranslator;
	}
	
	protected final KeyCache getKeyCache()
	{
		return keyCache;
	}

	private final Function<Object, Key> cachedInstanceToKeyFunction = new Function<Object, Key>()
	{
		public Key apply(Object instance)
		{
			return keyCache.getKey(instance);
		}
	};

	@Override
	public final FindCommand find()
	{
		return new StandardFindCommand(this);
	}

	@Override
	public final StoreCommand store()
	{
		return new StandardStoreCommand(this);
	}
	
	@Override
	public void activate(Object... instances)
	{
		activateAll(Arrays.asList(instances));
	}

	@Override
	public void activateAll(Collection<?> instances)
	{
		// TODO optimise this
		for (Object instance : instances)
		{
			refresh(instance);
		}
	}

	protected final void setIndexed(boolean indexed)
	{
		this.indexed = indexed;
	}

	final Entity instanceToEntity(Object instance, Key parentKey, String name)
	{
		String kind = fieldStrategy.typeToKind(instance.getClass());
		
		// push a new encode context
		KeySpecification existingEncodeKeySpec = encodeKeySpec;
		encodeKeySpec = new KeySpecification(kind, parentKey, name);

		keyCache.cacheKeyReferenceForInstance(instance, encodeKeySpec.toObjectReference());
			
		// translate fields to properties - sets parent and id on key
		PropertyTranslator encoder = encoder(instance);
		Set<Property> properties = encoder.typesafeToProperties(instance, Path.EMPTY_PATH, indexed);
		if (properties == null)
		{
			throw new IllegalStateException("Could not translate instance: " + instance);
		}

		// the key will now be set with id and parent
		Entity entity = createEntity();

		transferProperties(entity, properties);
		
		// we can store all entities for a single batch put
		if (batched != null)
		{
			batched.put(instance, entity);
		}
		// pop the encode context
		encodeKeySpec = existingEncodeKeySpec;
		
		return entity;
	}

	@SuppressWarnings("unchecked")
	final <T> Map<T, Entity> instancesToEntities(Collection<? extends T> instances, Object parent, boolean batch)
	{
		Key parentKey = null;
		if (parent != null)
		{
			parentKey= keyCache.getKey(parent);
		}
		
		Map<T, Entity> entities = new LinkedHashMap<T, Entity>(instances.size());
		if (batch)
		{
			batched = (Map<Object, Entity>) entities;
		}

		// TODO optimise
		for (T instance : instances)
		{
			// cannot define a key name
			Entity entity = instanceToEntity(instance, parentKey, null);
			entities.put(instance, entity);
		}
		
		if (batch)
		{
			batched = null;
		}
		
		return entities;
	}
	
	private void transferProperties(Entity entity, Collection<Property> properties)
	{
		for (Property property : properties)
		{
			// dereference object references
			Object value = property.getValue();
			value = dereferencePropertyValue(value);

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

	public static Object dereferencePropertyValue(Object value)
	{
		if (value instanceof ObjectReference<?>)
		{
			value = ((ObjectReference<?>)value).get();
		}
		else if (value instanceof List<?>)
		{
			// we know the value is a mutable list from ListTranslator
			@SuppressWarnings("unchecked")
			List<Object> values = (List<Object>) value;
			for (int i = 0; i < values.size(); i++)
			{
				Object item = values.get(i);
				if (item instanceof ObjectReference<?>)
				{
					// dereference the value and set it in-place
					Object dereferenced = ((ObjectReference<?>) item).get();
					values.set(i, dereferenced);  // replace the reference
				}
			}
		}
		return value;
	}
	

	public final Key store(Object instance, String name)
	{
		return store(instance, name, null);
	}

	public final Key store(Object instance)
	{
		return store(instance, null);
	}

	public final <T> T load(Class<T> type, Object key)
	{
		return load(type, key, null);
	}

	protected final <T> T internalLoad(Class<T> type, Object converted, Key parent)
	{
		assert activationDepthDeque.size() == 1;
		
		String kind = fieldStrategy.typeToKind(type);

		Key key;
		if (parent == null)
		{
			if (converted instanceof Long)
			{
				key = KeyFactory.createKey(kind, (Long) converted);
			}
			else
			{
				key = KeyFactory.createKey(kind, (String) converted);
			}
		}
		else
		{
			if (converted instanceof Long)
			{
				key = KeyFactory.createKey(parent, kind, (Long) converted);
			}
			else
			{
				key = KeyFactory.createKey(parent, kind, (String) converted);
			}
		}

		// needed to avoid sun generics bug "no unique maximal instance exists..."
		@SuppressWarnings("unchecked")
		T result = (T) keyToInstance(key, null);
		return result;
	}

	public final <T> QueryResultIterator<T> find(Class<T> type)
	{
		return find().type(type).returnResultsNow();
	}

	public final <T> QueryResultIterator<T> find(Class<T> type, Object ancestor)
	{
		return find().type(type).ancestor(ancestor).returnResultsNow();
	}

	final Query createQuery(Type type)
	{
		return new Query(fieldStrategy.typeToKind(type));
	}

	public final <T> T load(Key key)
	{
		@SuppressWarnings("unchecked")
		T result = (T) keyToInstance(key, null);
		return result;
	}

	final void internalUpdate(Object instance, Key key)
	{
		Entity entity = new Entity(key);
		
		// push a new encode context just to double check values and stop NPEs
		assert encodeKeySpec == null;
		encodeKeySpec = new KeySpecification();

		// translate fields to properties - sets parent and id on key
		Set<Property> properties = encoder(instance).typesafeToProperties(instance, Path.EMPTY_PATH, indexed);
		if (properties == null)
		{
			throw new IllegalStateException("Could not translate instance: " + instance);
		}

		transferProperties(entity, properties);
		
		// we can store all entities for a single batch put
		if (batched != null)
		{
			batched.put(instance, entity);
		}
		
		// pop the encode context
		encodeKeySpec = null;
		
		Key putKey = putEntity(entity);
		
		assert putKey.equals(key);
	}

	public final void deleteAll(Type type)
	{
		Query query = createQuery(type);
		query.setKeysOnly();
		FetchOptions options = FetchOptions.Builder.withChunkSize(100);
		Iterator<Entity> entities = servicePrepare(query).asIterator(options);
		Iterator<Key> keys = Iterators.transform(entities, entityToKeyFunction);
		Iterator<List<Key>> partitioned = Iterators.partition(keys, 100);
		while (partitioned.hasNext())
		{
			deleteKeys(partitioned.next());
		}
	}

	protected final void deleteKeys(Collection<Key> keys)
	{
		serviceDelete(keys);
		for (Key key : keys)
		{
			if (keyCache.containsKey(key))
			{
				keyCache.evictKey(key);
			}
		}
	}

	private final class StrategyObjectFieldTranslator extends ObjectFieldTranslator
	{
		private StrategyObjectFieldTranslator(TypeConverter converters)
		{
			super(converters);
		}
	
		@Override
		protected boolean indexed(Field field)
		{
			return StrategyObjectDatastore.this.storageStrategy.index(field);
		}
	
		@Override
		protected boolean stored(Field field)
		{
			return StrategyObjectDatastore.this.storageStrategy.store(field);
		}
	
		@Override
		protected Type typeFromField(Field field)
		{
			return StrategyObjectDatastore.this.fieldStrategy.typeOf(field);
		}
	
		@Override
		protected String fieldToPartName(Field field)
		{
			return StrategyObjectDatastore.this.fieldStrategy.name(field);
		}
	
		@Override
		protected PropertyTranslator encoder(Field field, Object instance)
		{
			return StrategyObjectDatastore.this.encoder(field, instance);
		}
	
		@Override
		protected PropertyTranslator decoder(Field field, Set<Property> properties)
		{
			return StrategyObjectDatastore.this.decoder(field, properties);
		}
	
		@Override
		protected Object createInstance(Class<?> clazz)
		{
			// if we are refreshing an instance do not create a new one
			Object instance = refresh;
			if (instance == null)
			{
				instance = super.createInstance(clazz);
			}
			refresh = null;
	
			// need to cache the instance immediately so instances can reference it
			if (keyCache.getInstance(decodeKey) == null)
			{
				// only cache first time - not for embedded components
				keyCache.cache(decodeKey, instance);
			}
	
			return instance;
		}
	
		@Override
		protected void onBeforeTranslate(Field field, Set<Property> childProperties)
		{
				if (activationDepthDeque.peek() > 0)
				{
					activationDepthDeque.push(StrategyObjectDatastore.this.activationStrategy.activationDepth(field, activationDepthDeque.peek() - 1));
				}
		}
	
		protected void onAfterTranslate(Field field, Object value)
		{
			activationDepthDeque.pop();
		}
	
		@Override
		protected void activate(Set<Property> properties, Object instance, Path path)
		{
			if (getActivationDepth() > 0)
			{
				super.activate(properties, instance, path);
			}
		}
	}

	private static final Function<Entity, Key> entityToKeyFunction = new Function<Entity, Key>()
	{
		public Key apply(Entity arg0)
		{
			return arg0.getKey();
		}
	};
}
