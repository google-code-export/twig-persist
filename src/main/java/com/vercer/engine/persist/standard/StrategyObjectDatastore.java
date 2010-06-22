package com.vercer.engine.persist.standard;

import java.io.NotSerializableException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.ConcurrentHashMap;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Transaction;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.google.common.collect.Maps;
import com.vercer.engine.persist.FindCommand;
import com.vercer.engine.persist.Path;
import com.vercer.engine.persist.Property;
import com.vercer.engine.persist.PropertyTranslator;
import com.vercer.engine.persist.StoreCommand;
import com.vercer.engine.persist.conversion.CombinedTypeConverter;
import com.vercer.engine.persist.conversion.DefaultTypeConverter;
import com.vercer.engine.persist.conversion.TypeConverter;
import com.vercer.engine.persist.strategy.ActivationStrategy;
import com.vercer.engine.persist.strategy.CombinedStrategy;
import com.vercer.engine.persist.strategy.FieldStrategy;
import com.vercer.engine.persist.strategy.RelationshipStrategy;
import com.vercer.engine.persist.strategy.StorageStrategy;
import com.vercer.engine.persist.translator.ChainedTranslator;
import com.vercer.engine.persist.translator.CoreTypesTranslator;
import com.vercer.engine.persist.translator.EnumTranslator;
import com.vercer.engine.persist.translator.ListTranslator;
import com.vercer.engine.persist.translator.NativeDirectTranslator;
import com.vercer.engine.persist.translator.ObjectFieldTranslator;
import com.vercer.engine.persist.translator.PolymorphicTranslator;
import com.vercer.util.LazyProxy;
import com.vercer.util.Reflection;

/**
 * Stateful layer responsible for caching key-object references and
 * creating a PropertyTranslator that can be configured using Strategy
 * instances.
 *
 * @author John Patterson <john@vercer.com>
 */
public class StrategyObjectDatastore extends AbstractStatelessObjectDatastore
{
//	private static final Logger logger = Logger.getLogger(StrategyObjectDatastore.class.getName());
	private static final int DEFAULT_ACTIVATION_DEPTH = Integer.MAX_VALUE;

	// state fields
	Transaction transaction;
	KeySpecification writeKeySpec;
	Key readKey;

	private final PropertyTranslator componentTranslator;
	private final PropertyTranslator polyMorphicComponentTranslator;
	private final PropertyTranslator parentTranslator;
	private final PropertyTranslator independantTranslator;
	private final PropertyTranslator keyFieldTranslator;
	private final PropertyTranslator childTranslator;
	private final ChainedTranslator valueTranslator;
	private final PropertyTranslator defaultTranslator;
	private final PropertyTranslator nullTranslator = new PropertyTranslator()
	{
		public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
		{
			return Collections.emptySet();
		}

		public Object propertiesToTypesafe(Set<Property> properties, Path path, Type type)
		{
			return NULL_VALUE;
		}
	};

	private final KeyCache keyCache;

	/**
	 * Flag that indicates we are associating instances with this session so do not store them
	 */
	private boolean associating;

	private boolean batching;
	private Map<Object, Entity> batched;

	private int depth;
	private Stack<Integer> activationDepth = new Stack<Integer>();

	private TypeConverter converter;
	private Object refreshing;

	private final RelationshipStrategy relationshipStrategy;
	private final FieldStrategy fieldStrategy;
	private final ActivationStrategy activationStrategy;
	private final StorageStrategy storageStrategy;


	public StrategyObjectDatastore(DatastoreService service, CombinedStrategy strategy)
	{
		this(service, strategy, strategy, strategy, strategy);
	}

	public StrategyObjectDatastore(DatastoreService datastore,
			RelationshipStrategy relationshipStrategy,
			StorageStrategy storageStrategy,
			ActivationStrategy activationStrategy,
			FieldStrategy fieldStrategy)
	{
		// use the protected constructor so we can configure the translator
		super(datastore);

		this.relationshipStrategy = relationshipStrategy;
		this.storageStrategy = storageStrategy;
		this.activationStrategy = activationStrategy;
		this.fieldStrategy = fieldStrategy;

		converter = createTypeConverter();

		activationDepth.push(DEFAULT_ACTIVATION_DEPTH);

		// central translator that reads fields and delegates to the others
		PropertyTranslator objectTranslator = new ObjectFieldTranslator(converter)
		{
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
			protected PropertyTranslator translator(Field field, Object instance)
			{
				return StrategyObjectDatastore.this.writeTranslator(field, instance);
			}

			@Override
			protected PropertyTranslator translator(Field field, Set<Property> properties)
			{
				return StrategyObjectDatastore.this.readTranslator(field, properties);
			}

			@Override
			protected Object createInstance(Class<?> clazz)
			{
				Object instance;
				if (refreshing == null)
				{
					instance = super.createInstance(clazz);
				}
				else
				{
					// we are refreshing existing instance so just reuse it
					instance = refreshing;
					
					// clear the the current refresh instance
					refreshing = null;
				}

				// need to cache the instance immediately so children can ref it
				if (keyCache.getCachedInstance(readKey) == null)
				{
					// only cache first time - not for embedded components
					keyCache.cache(readKey, instance);
				}

				return instance;
			}
			@Override
			protected void onBeforeTranslate(Field field, Set<Property> childProperties)
			{
				// only update activation depth when we are reading instances
				if (readKey != null)
				{
					if (activationDepth.peek() > 0)
					{
						activationDepth.push(StrategyObjectDatastore.this.activationStrategy.activationDepth(field, activationDepth.peek() - 1));
					}
				}
			}

			@Override
			protected void onAfterTranslate(Field field, Object value)
			{
				activationDepth.pop();
			}

			@Override
			protected void activate(Set<Property> properties, Object instance, Path path)
			{
				if (activationDepth.peek() > 0)
				{
					super.activate(properties, instance, path);
				}
			}

		};

		valueTranslator = createValueTranslatorChain();

		parentTranslator = new ParentEntityTranslator(this);
		independantTranslator = new ListTranslator(new IndependantEntityTranslator(this));
		keyFieldTranslator = new KeyFieldTranslator(this, valueTranslator, converter);
		childTranslator = new ListTranslator(new ChildEntityTranslator(this));
		componentTranslator = new ListTranslator(objectTranslator);
		polyMorphicComponentTranslator = new ListTranslator(new PolymorphicTranslator(objectTranslator, fieldStrategy));
		defaultTranslator = new ChainedTranslator(new ListTranslator(valueTranslator), getFallbackTranslator());

		setPropertyTranslator(objectTranslator);
		keyCache = new KeyCache();
	}

	protected PropertyTranslator readTranslator(Field field, Set<Property> properties)
	{
		return translator(field);
	}

	protected PropertyTranslator writeTranslator(Field field, Object instance)
	{
		return translator(field);
	}

	protected PropertyTranslator translator(Field field)
	{
		// optimise when we will not activate the fields
		if (activationDepth.peek() == 0)
		{
			return nullTranslator;
		}

		if (storageStrategy.entity(field))
		{
			if (relationshipStrategy.parent(field))
			{
				return parentTranslator;
			}
			else if (relationshipStrategy.child(field))
			{
				return childTranslator;
			}
			else
			{
				return independantTranslator;
			}
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
				return componentTranslator;
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
		return createDefaultTypeConverter();
	}

	protected final CombinedTypeConverter createDefaultTypeConverter()
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
		result.append(new CoreTypesTranslator());
		result.append(new EnumTranslator());
		return result;
	}

	@Override
	public final String typeToKind(Type type)
	{
		return fieldStrategy.typeToKind(type);
	}

	@Override
	protected final Type kindToType(String kind)
	{
		return fieldStrategy.kindToType(kind);
	}

	@Override
	protected void onBeforeEncode(Object instance)
	{
		depth++;
		if (keyCache.getCachedKey(instance) != null)
		{
			throw new IllegalStateException("Cannot store same instance twice: " + instance);
		}
		// create a place to put key details as we discover properties
		KeySpecification keySpec = new KeySpecification();

		// an existing write key spec indicates that we are a child
		if (writeKeySpec != null)
		{
			// make the existing spec the parent of the new current spec
			keySpec.setParentKeyReference(writeKeySpec.toObjectReference());
		}

		// cache the empty key details now in case a child references back to us
		keyCache.cacheKeyReferenceForInstance(instance, keySpec.toObjectReference());

		writeKeySpec = keySpec;
	}

	@Override
	protected void onAfterEncode(Object instance, Entity entity)
	{
		depth--;
		writeKeySpec = null;
		
		// we can store all entities for a single batch put
		if (batching)
		{
			if (batched == null)
			{
				batched = new LinkedHashMap<Object, Entity>();
			}
			batched.put(instance, entity);
		}
	}
	
	private static final Map<Class<?>, Field> keyFields = new ConcurrentHashMap<Class<?>, Field>();
	
	// null values are not permitted in a concurrent hash map so need a "missing" value
	private static final Field NO_KEY_FIELD;
	static
	{
		try
		{
			NO_KEY_FIELD = StrategyObjectDatastore.class.getDeclaredField("NO_KEY_FIELD");
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}
	
	protected void onAfterStore(Object instance, Key key) 
	{
		// replace the temp key ObjRef with the full key for this instance 
		keyCache.cache(key, instance);

		// TODO share fields with ObjectFieldTranslator
		Field field = null;
		if (keyFields.containsKey(instance.getClass()))
		{
			field = keyFields.get(instance.getClass());
		}
		else
		{
			List<Field> fields = Reflection.getAccessibleFields(instance.getClass());
			for (Field possible : fields)
			{
				if (possible.isAnnotationPresent(com.vercer.engine.persist.annotation.Key.class))
				{
					field = possible;
					break;
				}
			}
			
			if (field == null)
			{
				field = NO_KEY_FIELD;
			}
			keyFields.put(instance.getClass(), field);
		}
		
		try
		{
			// if there is a key field
			if (field != NO_KEY_FIELD)
			{
				// see if its current value is null or 0
				Object current = field.get(instance);
				if (current == null || current instanceof Number && ((Number) current).longValue() == 0)
				{
					Class<?> type = field.getType();
					Object idOrName = key.getId();
					
					// the key name could have been set explicitly when storing 
					if (idOrName == null)
					{
						idOrName = key.getName();
					}
					
					// convert the long or String to the declared key type
					Object converted = converter.convert(idOrName, type);
					field.set(instance, converted);
				}
			}
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	};

	/**
	 * Potentially store an entity in the datastore.
	 */
	@Override
	protected Key storeEntity(Entity entity)
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
		else if (batching)
		{
			// don't store anything yet if we are batching writes
			Key key = entity.getKey();
			if (depth > 1 && !key.isComplete())
			{
				// incomplete keys are no good to us
				throw new IllegalArgumentException("Batched child entity does not have complete key: " + entity);
			}

			// we will get the key after put
			return key;
		}
		else
		{
			// actually put the entity in the datastore
			return super.storeEntity(entity);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T toTypesafe(Entity entity, Predicate<String> propertyPredicate)
	{
		// cast needed to avoid sun generics bug "no unique maximal instance exists..."
		T typesafe = (T) keyCache.getCachedInstance(entity.getKey());
		if (typesafe == null)
		{
			Key current = readKey;
			readKey = entity.getKey();
			// cast needed to avoid sun generics bug "no unique maximal instance exists..."
			typesafe = (T) super.toTypesafe(entity, propertyPredicate);
			readKey = current;
		}

		return typesafe;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T keyToInstance(Key key, Predicate<String> propertyPredicate)
	{
		// only cache full instances
		T typesafe = null;
		if (propertyPredicate == null)
		{
			typesafe = (T) keyCache.getCachedInstance(key);
		}

		if (typesafe == null)
		{
			typesafe = (T) super.keyToInstance(key, propertyPredicate);
		}

		return typesafe;
	}
	
	@Override
	protected Entity keyToEntity(Key key)
	{
		if (activationDepth.peek() > 0)
		{
			return super.keyToEntity(key);
		}
		else
		{
			// don't load entity if it will not be activated
			return new Entity(key);
		}
	}
	
	@Override
	protected Entity createEntity(KeySpecification specification)
	{
		// add any key info we have gathered while writing the fields
		writeKeySpec.merge(specification);
		return super.createEntity(writeKeySpec);
	}

	protected boolean propertiesIndexedByDefault()
	{
		return true;
	}

	public final void disassociate(Object reference)
	{
		keyCache.evictInstance(reference);
	}

	public void disassociateAll()
	{
		keyCache.clearKeyCache();
	}

	public final void associate(Object instance, Key key)
	{
		keyCache.cache(key, instance);
	}

	public final void associate(Object instance)
	{
		// convert the instance so we can get its key and children
		associating = true;
		store(instance);
		associating = false;
	}

	@Override
	protected RuntimeException newExceptionOnTranslateWrite(Exception e, Object instance)
	{
		if (e instanceof RuntimeException && e instanceof NotSerializableException == false)
		{
			return (RuntimeException) e;
		}
		else
		{
			String message = "There was a problem translating instance " + instance + "\n";
			message += "Make sure instances are either Serializable or configured as components or entities.";
			return new IllegalStateException(message, e);
		}
	}


	public final <T> T load(Class<T> type, Object key, Object parent)
	{
		assert activationDepth.size() == 1;

		Object converted;
		if (Number.class.isAssignableFrom(key.getClass()))
		{
			converted = converter.convert(key, Long.class);
		}
		else
		{
			converted = converter.convert(key, String.class);
		}

		Key parentKey = null;
		if (parent != null)
		{
			parentKey = keyCache.getCachedKey(parent);
		}
		return internalLoad(type, converted, parentKey);
	}

//	public final <T, K> Map<K, T>  load(Class<? extends T> type, Collection<? extends K> keys)
//	{
//		assert activationDepth.size() == 1;
//
//		List<Object> converted = new ArrayList<Object>(keys.size());
//		for (K key : keys)
//		{
//			if (Number.class.isAssignableFrom(key.getClass()))
//			{
//				converted.add(converter.convert(key, Long.class));
//			}
//			else
//			{
//				converted.add(converter.convert(key, String.class));
//			}
//		}
//
//		return internalLoadAll(type, converted);
//	}

	public final void update(Object instance)
	{
		Key key = keyCache.getCachedKey(instance);
		if (key == null)
		{
			throw new IllegalArgumentException("Can only update instances loaded from this session");
		}
		update(instance, key);
	}

	public void storeOrUpdate(Object instance)
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

	public void storeOrUpdate(Object instance, Object parent)
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

	public final void delete(Object instance)
	{
		Key key= keyCache.getCachedKey(instance);
		if (key == null)
		{
			throw new IllegalArgumentException("Instance " + instance + " is not associated");
		}
		deleteKeys(Collections.singleton(key));
	}

	public final void deleteAll(Collection<?> instances)
	{
		deleteKeys(Collections2.transform(instances, instanceToKey));
	}

	@Override
	protected void onAfterDelete(Collection<Key> keys)
	{
		for (Key key : keys)
		{
			if (keyCache.containsKey(key))
			{
				keyCache.evictKey(key);
			}
		}
	}

//	Object getInstanceFromCacheOrLoad(Key key)
//	{
//		Object instance = keyCache.getCachedInstance(key);
//		if (instance == null)
//		{
//			instance = load(key);
//			assert instance != null;
//		}
//		return instance;
//	}

	Key getKeyFromCacheOrStore(final Object instance)
	{
		Key key = keyCache.getCachedKey(instance);
		if (key == null)
		{
			key = store(instance);
		}
		return key;
	}

	public final Key store(Object instance, Object parent)
	{
		Key parentKey = keyCache.getCachedKey(parent);
		return store(instance, parentKey);
	}

	public final Key store(Object instance, String name, Object parent)
	{
		Key parentKey = null;
		if (parent != null)
		{
			parentKey = keyCache.getCachedKey(parent);
		}
		return store(instance, parentKey, name);
	}

	public final Key associatedKey(Object instance)
	{
		return keyCache.getCachedKey(instance);
	}

	public final <T> Map<T, Key> storeAll(Collection<? extends T> instances)
	{
		return storeAll(instances, (Key) null);
	}

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
			onAfterStore(instance, key);
			result.put(instance, key);
		}
		return result;
	}

	final <T> Map<T, Entity> instancesToEntities(Collection<? extends T> instances, Object parent, boolean batch)
	{
		Key parentKey = null;
		if (parent != null)
		{
			parentKey = keyCache.getCachedKey(parent);
		}
		if (batch)
		{
			// pretend we are storing them to capture all referenced instances
			batching = true;
			for (T instance : instances)
			{
				store(instance, parentKey);
			}
			batching = false;

			@SuppressWarnings("unchecked")
			Map<T, Entity> result = (Map<T, Entity>) batched;
			batched = null;
			return result;
		}
		else
		{
			Map<T, Entity> entities = new LinkedHashMap<T, Entity>(instances.size());
			for (T instance : instances)
			{
				// cannot define a key name
				Entity entity = instanceToEntity(instance, parentKey, null);
				entities.put(instance, entity);
			}
			return entities;
		}
	}
	
	public final void refresh(Object instance)
	{
		Key key = associatedKey(instance);

		if (key == null)
		{
			throw new IllegalStateException("Instance not associated with session");
		}

		// so it is not loaded from the cache
		disassociate(instance);

		// instance will be reused instead of creating new
		refreshing = instance;

		// load from datastore into the instance
		Object loaded = load(key);

		if (loaded == null)
		{
			throw new IllegalStateException("Instance to be refreshed could not be loaded");
		}
	}

	public final int getActivationDepth()
	{
		return activationDepth.peek();
	}

	public final void setActivationDepth(int depth)
	{
		activationDepth.clear();
		activationDepth.push(depth);
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
		return componentTranslator;
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

	private final Function<Object, Key> instanceToKey = new Function<Object, Key>()
	{
		public Key apply(Object instance)
		{
			return keyCache.getCachedKey(instance);
		}
	};

	public final FindCommand find()
	{
		assert activationDepth.size() == 1;
		return new StandardFindCommand(this);
	}

	public final StoreCommand store()
	{
		return new StandardStoreCommand(this);
	}

	public final Transaction getTransaction()
	{
		return transaction;
	}

	public final Transaction beginTransaction()
	{
		if (transaction != null && transaction.isActive())
		{
			throw new IllegalStateException("Already in active transaction");
		}
		transaction = getService().beginTransaction();
		return transaction;
	}

	public final void removeTransaction()
	{
		transaction = null;
	}
	
	@Override
	protected void serviceDelete(Collection<Key> keys)
	{
		if (transaction == null)
		{
			super.serviceDelete(keys);
		}
		else
		{
			getService().delete(transaction, keys);
		}
	}
	
	@Override
	protected Entity serviceGet(Key key) throws EntityNotFoundException
	{
		if (transaction == null)
		{
			return super.serviceGet(key);
		}
		else
		{
			return getService().get(transaction, key);
		}
	}
	
	@Override
	protected PreparedQuery servicePrepare(Query query)
	{
		if (transaction == null)
		{
			return super.servicePrepare(query);
		}
		else
		{
			return getService().prepare(transaction, query);
		}
	}
	
	@Override
	protected Key servicePut(Entity entity)
	{
		if (transaction == null)
		{
			return super.servicePut(entity);
		}
		else
		{
			return getService().put(transaction, entity);
		}
	}
	
	@Override
	protected List<Key> servicePut(Iterable<Entity> entities)
	{
		if (transaction == null)
		{
			return super.servicePut(entities);
		}
		else
		{
			return getService().put(transaction, entities);
		}
	}
}
