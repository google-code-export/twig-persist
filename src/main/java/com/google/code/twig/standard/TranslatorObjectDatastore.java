package com.google.code.twig.standard;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.code.twig.LoadCommand.CacheMode;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.Settings;
import com.google.code.twig.annotation.Root;
import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.translator.ChainedTranslator;
import com.google.code.twig.translator.FieldTranslator;
import com.google.code.twig.translator.PolymorphicTranslator;
import com.google.code.twig.util.EntityToKeyFunction;
import com.google.code.twig.util.Pair;
import com.google.code.twig.util.Reflection;
import com.google.code.twig.util.Strings;
import com.google.code.twig.util.generic.Generics;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterators;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.vercer.convert.TypeConverter;

/**
 * TODO split this into a base impl with caching, activation etc and the translator
 */
public abstract class TranslatorObjectDatastore extends BaseObjectDatastore
{
	// keeps track of which instances are associated with which keys
	protected final KeyInstanceCache keyCache = new KeyInstanceCache();

	// ensure only used be a single thread
	protected Thread thread;

	// translators are selected for particular fields by the configuration
	private final ObjectFieldTranslator objectFieldTranslator;
	private final ContainerTranslator containerTranslator;
	private final ChainedTranslator embedTranslator;
	private final PropertyTranslator polymorphicComponentTranslator;
	private final PropertyTranslator parentTranslator;
	private final PropertyTranslator independantTranslator;
	private final PropertyTranslator idFieldTranslator;
	private final PropertyTranslator childTranslator;
	private final ChainedTranslator valueTranslatorChain;
	private final PropertyTranslator keyFieldTranslator;
	private final ChainedTranslator defaultTranslator;

	private static final Map<Class<?>, Field> idFields = new ConcurrentHashMap<Class<?>, Field>();
	private static final Map<Class<?>, Field> keyFields = new ConcurrentHashMap<Class<?>, Field>();

	/**************State fields********************/

	// key details are updated as the current instance is encoded
	protected KeyDetails encodeKeyDetails;

	// the key of the currently decoding entity
	protected Key decodeKey;

	// current activation depth
	final int defaultActivationDepth;
	
	final boolean defaultIndexFields;

	// indicates we are only associating instances so do not store them
	boolean associating = false;

	// when we associating should instances be activated
	boolean activate = true;

	// use for the next instance to be decoded instead of creating
	protected Object refresh;
	
	boolean denormalising;
	
	// TODO - this is very fragile! need more reliable way to reference current command
	// main problem is during iteration of results the last command will change
	// so it must be reset on every iteration. Also must always remember the current
	// command whenever creating a new command inside the framework and then reset it
	// TODO hide this - just for testing
	
	// allow current command to be manipulated by field annotations
	public StandardCommand command;

	Map<String, Iterator<Key>> allocatedIdRanges;

	private final Configuration configuration;

	public TranslatorObjectDatastore(Settings settings, Configuration configuration, int activation, boolean index)
	{
		super(settings);
		this.configuration = configuration;
		defaultActivationDepth = activation;
		defaultIndexFields = index;

		this.thread = Thread.currentThread();
		
		TypeConverter converter = getTypeConverter();

		// top level translator which examines object field values
		objectFieldTranslator = new ObjectFieldTranslator(converter);

		containerTranslator = new ContainerTranslator(this);
		
		// simple values encoded as a single property
		valueTranslatorChain = createValueTranslatorChain();

		// referenced instances stored as separate entities
		parentTranslator = new ParentRelationTranslator(this);
		childTranslator = new ChildRelationTranslator(this);
		independantTranslator = new RelationTranslator(this);

		// @Id and @GaeKey fields
		keyFieldTranslator = new KeyTranslator(this);
		idFieldTranslator = new IdFieldTranslator(this, valueTranslatorChain);

		// embed a field value in the current entity
		embedTranslator = new ChainedTranslator();
		embedTranslator.append(valueTranslatorChain); // always encode values as simple properties
		embedTranslator.append(new IterableTranslator(this, embedTranslator));
		embedTranslator.append(new MapTranslator(this, embedTranslator, converter));
		embedTranslator.append(objectFieldTranslator);

		// if the property has a key then the model was changed
		// from a reference to embedded so decode the reference
		embedTranslator.append(independantTranslator);

		// polymorphic translator - try simple values and then embedded components
//		ChainedTranslator polymorphicValueTranslator = new ChainedTranslator(valueTranslatorChain, embedTranslator);
		PropertyTranslator polymorphicTranslator = new PolymorphicTranslator(embedTranslator, configuration);

		// allow it to handle maps and lists of polymorphic instances
		polymorphicComponentTranslator = new ChainedTranslator(
				new IterableTranslator(this, polymorphicTranslator),
				new MapTranslator(this, polymorphicTranslator, converter),
				polymorphicTranslator);

		// by default, translate simple values and then try the fall-back if that fails
		defaultTranslator = new ChainedTranslator();
		defaultTranslator.append(new IterableTranslator(this, defaultTranslator));
		defaultTranslator.append(new MapTranslator(this, defaultTranslator, converter));
		defaultTranslator.append(valueTranslatorChain);
		defaultTranslator.append(getFallbackTranslator());
	}

	@Override
	public StandardFindCommand find()
	{
		return new StandardFindCommand(this);
	}

	@Override
	public StandardStoreCommand store()
	{
		return new StandardStoreCommand(this).update(false);
	}

	@Override
	public StandardLoadCommand load()
	{
		return new StandardLoadCommand(this);
	}

	@Override
	public final Key store(Object instance)
	{
		assert instance != null;
		return store().instance(instance).now();
	}

	@Override
	public final Key store(Object instance, long id)
	{
		assert instance != null;
		assert id > 0;
		return store().instance(instance).id(id).now();
	}

	@Override
	public final Key store(Object instance, String id)
	{
		assert instance != null;
		assert id != null;
		assert id.length() > 0;
		return store().instance(instance).id(id).now();
	}

	@Override
	public final <T> Map<T, Key> storeAll(Collection<? extends T> instances)
	{
		return store().instances(instances).now();
	}

	public final <T> QueryResultIterator<T> find(Class<? extends T> type)
	{
		return find().type(type).now();
	}

	public final <T> QueryResultIterator<T> find(Class<? extends T> type, String field, Object value)
	{
		return find().type(type).addFilter(field, FilterOperator.EQUAL, value).now();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T load(Key key)
	{
		return (T) load().key(key).now();
	}

	@Override
	public final <T> T load(Class<? extends T> type, Object id)
	{
		return load().type(type).id(id).now();
	}

	@Override
	public final <T> Map<?, T> loadAll(Class<? extends T> type, Collection<?> ids)
	{
		return load().type(type).ids(ids).now();
	}

	@Override
	public final void update(Object instance, boolean cascade)
	{
		assert instance != null;

		// store but set the internal update flag
		store().update(true)
			.instance(instance)
			.cascaded(cascade ? new HashSet<Object>() : null)
			.date(cascade ? new Date() : null)
			.now();
	}

	@Override
	public void update(Object instance)
	{
		update(instance, false);
	}
	
	@Override
	public void updateAll(Collection<?> instances, boolean cascade)
	{
		assert instances != null;
		
		// store but set the internal update flag
		store().update(true)
			.instances(instances)
			.cascaded(cascade ? new HashSet<Object>() : null)
			.date(cascade ? new Date() : null)
			.now();
	}

	@Override
	public void updateAll(Collection<?> instances)
	{
		updateAll(instances, false);
	}
	
	@Override
	public final void delete(Object instance)
	{
		assert instance != null;
		Key key = keyCache.getKey(instance);
		if (key == null)
		{
			throw new IllegalArgumentException("Instance " + instance + " is not associated");
		}
		deleteKeys(Collections.singleton(key));
	}

	@Override
	public final void deleteAll(Collection<?> instances)
	{
		assert instances != null;
		deleteKeys(Collections2.transform(instances, cachedInstanceToKeyFunction));
	}

	/**
	 * The first extension point called to get the decode translator
	 * @return {@link #translator(Class)}
	 */
	protected PropertyTranslator decoder(Entity entity)
	{
		return translator(configuration.kindToType(entity.getKind()));
	}

	/**
	 * The first extension point called to get the encode translator
	 * @return {@link #translator(Class)}
	 */
	protected PropertyTranslator encoder(Object instance)
	{
		return translator(instance.getClass());
	}

	/**
	 * Common extension point for encode and decode translators
	 * @return {@link #objectFieldTranslator}
	 */
	protected PropertyTranslator translator(Class<?> instanceClass)
	{
		return objectFieldTranslator;
	}
	
	/**
	 * Called from {@link ObjectFieldTranslator} to allow extension
	 * @param field The current field being decoded by {@link ObjectFieldTranslator}
	 * @param properties The subset of properties for this field
	 * @return {@link #translator(Field)}
	 */
	protected PropertyTranslator decoder(Field field, Set<Property> properties)
	{
		return translator(field);
	}

	/**
	 * Called from {@link ObjectFieldTranslator} to allow extension
	 * @param field The current field being encoded by {@link ObjectFieldTranslator}
	 * @param instance The object value to serialize as {@link Property}'s
	 * @return {@link #translator(Field)}
	 */
	protected PropertyTranslator encoder(Field field, Object instance)
	{
		return translator(field);
	}

	/**
	 * @param field The current field value being encoded or decoded
	 * @return The translator for this field
	 */
	protected PropertyTranslator translator(Field field)
	{
		PropertyTranslator result;
		if (configuration.entity(field))
		{
			if (configuration.parent(field))
			{
				result = parentTranslator;
			}
			else if (configuration.child(field))
			{
				result = childTranslator;
			}
			else
			{
				result = independantTranslator;
			}

			// should we denormalise some paths
			String[] paths = configuration.denormalise(field);
			if (paths != null)
			{
				DenormaliseTranslator denormalizer = new DenormaliseTranslator(this, result, Sets.newHashSet(paths));
				result = new ChainedTranslator(new MapTranslator(this, denormalizer, getTypeConverter()), denormalizer);
			}
		}
		else if (field.isAnnotationPresent(Root.class))
		{
			result = containerTranslator;
		}
		else if (configuration.id(field))
		{
			result = idFieldTranslator;
		}
		else if (configuration.embed(field))
		{
			if (configuration.polymorphic(field))
			{
				result = polymorphicComponentTranslator;
			}
			else
			{
				result = embedTranslator;
			}
		}
		else if (configuration.key(field))
		{
			result = keyFieldTranslator;
		}
		else
		{
			result = defaultTranslator;
		}

		// if there are too many properties, serialize them 
		int serializationThreshold = configuration.serializationThreshold(field);
		if (serializationThreshold >= 0)
		{
			result = new SerializeTranslator(result, serializationThreshold);
		}

		return result;
	}

	/**
	 * @return The translator which is used if no others handle the instance
	 */
	protected abstract PropertyTranslator getFallbackTranslator();

	/**
	 * @return The translator that is used for single items by default
	 */
	protected abstract ChainedTranslator createValueTranslatorChain();

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
	public <T> T associate(T instance, Key key, long version)
	{
		@SuppressWarnings("unchecked")
		T existing = (T) keyCache.getInstance(key);
		if (existing != null)
		{
			return existing;
		}
		
		// negative version indicates value was not loaded
		keyCache.cache(key, instance, -version);
		return instance;
	}

	@Override
	public <T> T associate(T instance, Key key)
	{
		return associate(instance, key, 0);
	}

	@Override
	public <T> T associate(T instance)
	{
		return associate(instance, false);
	}

	@Override
	public <T> T associate(T instance, boolean activated)
	{
		return associate(instance, activated ? 1 : 0, null, null);
	}

	@Override
	public <T> T associate(Class<T> type, Object id)
	{
		return createSetKeyAssociate(type, id);
	}

	private <T> T createSetKeyAssociate(Class<T> type, Object id)
	{
		T instance = (T) createInstance(type);

		Field keyField = idField(type);

		try
		{
			keyField.set(instance, id);
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}

		return associate(instance);
	}
	
	public <T> T associate(T instance, boolean activated, Object parent) 
	{
		return associate(instance, 1, parent, null);
	};

	@Override
	public <T> T associate(T instance, long version, Object parent, Object id)
	{
		try
		{
			// ignore instances that are already associated
			if (isAssociated(instance))
			{
				return instance;
			}

			// flag that we should not really store the instance
			this.associating = true;
			this.activate = version > 0;

			// remember the real current command
			StandardCommand existingCommand = command;
			
			// use a dummy store command to analyse this and all referenced instances
			StandardSingleStoreCommand<Object> dummy = store().instance(null);
			if (parent != null)
			{
				dummy.parent(parent);
			}

			// create key from @Id and @Parent fields
			Key key = dummy.instanceToKey(instance, id);

			// replace the real command
			command = existingCommand;
			
			// check if there is already an instance with this key
			@SuppressWarnings("unchecked")
			T existing = (T) keyCache.getInstance(key);

			if (existing != null)
			{
				return existing;
			}
			else
			{
				// negative version indicates value was not loaded
				keyCache.cache(key, instance, -version);
				return instance;
			}
		}
		finally
		{
			this.associating = false;
			this.activate = true;
		}
	}

	@Override
	public <T> Collection<T> associateAll(Collection<T> instances)
	{
		// no point using bulk store() because it never hits datastore
		Builder<T> builder = ImmutableList.<T>builder();
		for (T instance : instances)
		{
			builder.add(this.associate(instance));
		}
		return builder.build();
	}

	@Override
	public final Key associatedKey(Object instance)
	{
		return keyCache.getKey(instance);
	}
	
	@Override
	public <T> T associatedInstance(Key key)
	{
		return keyCache.getInstance(key);
	}
	
	@Override
	public long version(Object instance)
	{
		return keyCache.version(instance);
	}

	@Override
	public boolean isAssociated(Object instance)
	{
		// the key may be incomplete so getKey is not safe
		return keyCache.getKeyReference(instance) != null;
	}

	@Override
	public void flushBatchedOperations()
	{
		List<Key> keys = flushEntities(getDefaultSettings());

		// keys hash codes may have changed after put
		keyCache.rehashKeys();

		// set the ids of any instances that are still in memory
		for (Key key : keys)
		{
			Object instance = keyCache.getInstance(key);

			// instance may have been garbage collected
			if (instance != null)
			{
				StandardCommonStoreCommand.updateKeyState(instance, key, this);
			}
		}
	}

	public abstract TypeConverter getTypeConverter();

	protected final PropertyTranslator getIndependantTranslator()
	{
		return independantTranslator;
	}

	protected ChainedTranslator getValueTranslatorChain()
	{
		return this.valueTranslatorChain;
	}

	protected final PropertyTranslator getChildTranslator()
	{
		return childTranslator;
	}

	protected final PropertyTranslator getParentTranslator()
	{
		return parentTranslator;
	}

	protected final PropertyTranslator getPolymorphicTranslator()
	{
		return polymorphicComponentTranslator;
	}

	protected final PropertyTranslator getFieldTranslator()
	{
		return objectFieldTranslator;
	}

	protected final PropertyTranslator getEmbeddedTranslator()
	{
		return embedTranslator;
	}

	protected final PropertyTranslator getIdFieldTranslator()
	{
		return idFieldTranslator;
	}

	protected final PropertyTranslator getDefaultTranslator()
	{
		return defaultTranslator;
	}

	protected final KeyInstanceCache getKeyCache()
	{
		return keyCache;
	}

	private final Function<Object, Key> cachedInstanceToKeyFunction = new Function<Object, Key>()
	{
		@Override
		public Key apply(Object instance)
		{
			return keyCache.getKey(instance);
		}
	};

	@Override
	public void activate(Object instance)
	{
		if (keyCache.isActivatable(instance))
		{
			doRefreshActivate(ImmutableList.of(instance));
		}
	}

	@Override
	public boolean isActivatable(Object instance)
	{
		return keyCache.isActivatable(instance);
	}

	@Override
	public final void refresh(Object instance)
	{
		doRefreshActivate(ImmutableList.of(instance));

		// do the check after refresh so we know the instance is associated
		if (!isActivated(instance))
		{
			throw new IllegalStateException("Use activate for unactivated instance " + instance);
		}
	}

	private void doRefreshActivate(Collection<?> instances)
	{
		if (instances.isEmpty()) return;

		Collection<Key> keys = new HashSet<Key>(instances.size());
		for (Object instance : instances)
		{
			Key key = associatedKey(instance);
			if (key == null)
			{
				throw new IllegalStateException("Instance not associated: " + instance);
			}

			keys.add(key);
		}

		// load from datastore into the refresh instances
		Map<Key, Object> loaded = load().keys(keys).refresh().now();

		if (loaded.size() != keys.size())
		{
			throw new IllegalStateException("Some instances were not found for keys " + keys + " found " + loaded);
		}
	}

	@Override
	public void refreshAll(Collection<?> instances)
	{
		doRefreshActivate(instances);
	}

	@Override
	public void activateAll(Collection<?> instances)
	{
		if (instances.isEmpty()) return;

		// be careful only to create another collection if needed
		// cannot filter instances because isActivated can change
		Collection<Object> unactivated = null;
		for (Object instance : instances)
		{
			if (isAssociated(instance) && !isActivated(instance))
			{
				if (unactivated == null)
				{
					unactivated = new ArrayList<Object>(instances.size());
				}
				unactivated.add(instance);
			}
		}

		if (unactivated != null)
		{
			doRefreshActivate(unactivated);
		}
	}

	@Override
	public boolean isActivated(Object instance)
	{
		return keyCache.isActivated(instance);
	}

	final Query createQuery(Class<?> type)
	{
		return new Query(configuration.typeToKind(type));
	}

	public final void deleteAll(Class<?> type)
	{
		Query query = createQuery(type);
		query.setKeysOnly();
		FetchOptions options = FetchOptions.Builder.withChunkSize(100);
		Iterator<Entity> entities = servicePrepare(query, null).asIterator(options);
		Iterator<Key> keys = Iterators.transform(entities, entityToKeyFunction);
		Iterator<List<Key>> partitioned = Iterators.partition(keys, 100);
		while (partitioned.hasNext())
		{
			deleteKeys(partitioned.next());
		}
	}

	@Override
	public void deleteKeys(Collection<Key> keys)
	{
		// TODO is auto the right setting here?
		serviceDelete(keys, CacheMode.AUTO);
		for (Key key : keys)
		{
			if (keyCache.containsKey(key))
			{
				keyCache.evictKey(key);
			}
		}
	}
	
	@Override
	public void deleteKey(Key key)
	{
		deleteKeys(ImmutableList.of(key));
	}

	// permanent cache of class fields to reduce reflection
	private static Map<Class<?>, Collection<Field>> classFields = Maps.newConcurrentMap();
	private static Map<Class<?>, Constructor<?>> constructors = Maps.newConcurrentMap();

	// top level translator that uses the Settings to decide which translator
	// to use for each Field value.
	public final class ObjectFieldTranslator extends FieldTranslator
	{
		private ObjectFieldTranslator(TypeConverter converters)
		{
			super(converters);
		}

		private Comparator<Field> fieldComparator = new Comparator<Field>()
		{
			@Override
			public int compare(Field o1, Field o2)
			{
				return configuration.name(o1).compareTo(configuration.name(o2));
			}
		};

		// TODO put this in a dedicated meta-data holder
		@Override
		protected Collection<Field> getSortedAccessibleFields(Class<?> clazz)
		{
			// stored unsorted because order depends on configuration.name()
			Collection<Field> unsorted = classFields.get(clazz);
			if (unsorted == null)
			{
				//cache costly reflection
				unsorted = Reflection.getAccessibleFields(clazz);
				classFields.put(clazz, unsorted);
			}

			Set<Field> fields = new TreeSet<Field>(fieldComparator);
			fields.addAll(unsorted);
			return fields;
		}
		
		@Override
		protected Constructor<?> getDefaultConstructor(Class<?> clazz) throws NoSuchMethodException
		{
			Constructor<?> constructor = constructors.get(clazz);
			if (constructor == null)
			{
				// use no-args constructor
				constructor = clazz.getDeclaredConstructor();

				// allow access to private constructor
				if (!constructor.isAccessible())
				{
					constructor.setAccessible(true);
				}

				constructors.put(clazz, constructor);
			}
			return constructor;
		}

		@Override
		protected boolean indexed(Field field)
		{
			Boolean index = TranslatorObjectDatastore.this.configuration.index(field);
			return index == null ? defaultIndexFields : index;
		}

		@Override
		protected boolean stored(Field field)
		{
			return TranslatorObjectDatastore.this.configuration.store(field);
		}

		@Override
		protected Type type(Field field)
		{
			// TODO look up the type in current Registration
			return TranslatorObjectDatastore.this.configuration.typeOf(field);
		}

		@Override
		protected String fieldToPartName(Field field)
		{
			return TranslatorObjectDatastore.this.configuration.name(field);
		}

		@Override
		protected PropertyTranslator encoder(Field field, Object instance)
		{
			return TranslatorObjectDatastore.this.encoder(field, instance);
		}

		@Override
		protected PropertyTranslator decoder(Field field, Set<Property> properties)
		{
			return TranslatorObjectDatastore.this.decoder(field, properties);
		}

		protected final Object defautCreateInstance(Class<?> clazz)
		{
			return super.createInstance(clazz);
		}

		@Override
		protected final Object createInstance(Class<?> clazz)
		{
			Object result;
			if (refresh == null)
			{
				// give subclasses a chance to create the instance
				result = TranslatorObjectDatastore.this.createInstance(clazz);
				
				// only cache persistent instance - not embedded components
				if (keyCache.getInstance(decodeKey) == null)
				{
					// we have not activated the instance yet
					keyCache.cache(decodeKey, result, 0);
				}
			}
			else
			{
				assert clazz.isInstance(refresh);
				result = refresh;
				refresh = null;
			}

			return result;
		}
		
		@Override
		protected void decodeField(Object instance, Field field, Path path, Set<Property> properties)
		{
			// temporarily change the activation depth if this field has one set
			StandardDecodeCommand<?> decode = (StandardDecodeCommand<?>) command;
			int depth = configuration.activationDepth(field, decode.getDepth());
			decode.setDepth(depth);

			final Object value = Reflection.get(field, instance);

			// when denormalising we enhance the existing value
			if (denormalising)
			{
				refresh = value;
			}
			else if (value instanceof Collection<?>)
			{
				// reuse existing collection instances
				Collection<?> collection = (Collection<?>) value;
				collection.clear();
				refresh = collection;
			}
			else if (value instanceof Map<?, ?>)
			{
				// reuse existing map instances
				Map<?, ?> map = (Map<?, ?>) value;
				map.clear();
				refresh = map;
			}
			super.decodeField(instance, field, path, properties);
			
			refresh = null;
		}
	}
	
	// TODO roll this meta-data into a single class that is looked up once only
	Field idField(Class<?> type)
	{
		Field result = null;
		if (idFields.containsKey(type))
		{
			result = idFields.get(type);
		}
		else
		{
			List<Field> fields = Reflection.getAccessibleFields(type);
			for (Field field : fields)
			{
				if (configuration.id(field))
				{
					result = field;
					break;
				}
			}

			// null cannot be stored in a concurrent hash map
			if (result == null)
			{
				result = NO_ID_FIELD;
			}
			idFields.put(type, result);
		}

		if (result == NO_ID_FIELD)
		{
			return null;
		}
		else
		{
			return result;
		}
	}

	Field keyField(Class<?> type)
	{
		Field result = null;
		if (keyFields.containsKey(type))
		{
			result = keyFields.get(type);
		}
		else
		{
			List<Field> fields = Reflection.getAccessibleFields(type);
			for (Field field : fields)
			{
				if (configuration.key(field))
				{
					result = field;
					break;
				}
			}

			// null cannot be stored in a concurrent hash map
			if (result == null)
			{
				result = NO_ID_FIELD;
			}
			keyFields.put(type, result);
		}

		if (result == NO_ID_FIELD)
		{
			return null;
		}
		else
		{
			return result;
		}
	}

	/**
	 * Create a new instance which will have its fields populated from stored properties.
	 *
	 * @param clazz The type to create
	 * @return A new instance or null to use the default behaviour
	 */
	@SuppressWarnings("unchecked")
	protected <T> T createInstance(Class<T> clazz)
	{
		return (T) objectFieldTranslator.defautCreateInstance(clazz);
	}

	public Configuration getConfiguration()
	{
		return configuration;
	}

	// null values are not permitted in a concurrent hash map so
	// need a special value to represent a missing field
	private static final Field NO_ID_FIELD;
	static
	{
		try
		{
			NO_ID_FIELD = TranslatorObjectDatastore.class.getDeclaredField("NO_ID_FIELD");
		}
		catch (Exception e)
		{
			throw new IllegalStateException(e);
		}
	}

	protected static final Function<Entity, Key> entityToKeyFunction = new EntityToKeyFunction();
	

	public Pair<Field, String> getFieldAndPropertyForPath(String fieldPathName, Type type)
	{
		Field field = null;

		// get the stored path from the object navigation path
		String[] fieldNames = Strings.split(fieldPathName, false, '.');
		Path path = Path.EMPTY_PATH;
		String property = null;
		for (String fieldName : fieldNames)
		{
			field = null;
			Class<?> erased = Generics.erase(type);

			// collections use the element type
			if (Collection.class.isAssignableFrom(erased))
			{
				type = ((ParameterizedType) Generics.getExactSuperType(type, Collection.class)).getActualTypeArguments()[0];
				erased = Generics.erase(type);
			}

			// get fields that were already cached in any order
			// TODO cache fields? need to take timings. probably not worth it for filters
			Collection<Field> fields = Reflection.getAccessibleFields(erased);
			for (Field candidate : fields)
			{
				if (candidate.getName().equals(fieldName))
				{
					field = candidate;
				}
			}

			if (field == null)
			{
				throw new IllegalArgumentException("Could not find field " + fieldName + " in type " + type);
			}

			// field type could have type variable if defined in superclass
			type = Generics.getExactFieldType(field, type);

			// if the field is an @Id we need to create a Key value
			if (configuration.id(field))
			{
				if (!path.isEmpty())
				{
					throw new IllegalArgumentException("Id field must be at root of filter");
				}
				property = Entity.KEY_RESERVED_PROPERTY;
				break;
			}

			// the property name stored in the datastore may use a short name
			String propertyName = configuration.name(field);
			path = new Path.Builder(path).field(propertyName).build();
		}

		// path will only be empty if we are filtering on id
		if (!path.isEmpty())
		{
			assert property == null;
			property = path.toString();
		}

		return new Pair<Field, String>(field, property);
	}

}
