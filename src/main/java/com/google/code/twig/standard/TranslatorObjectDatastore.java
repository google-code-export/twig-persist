package com.google.code.twig.standard;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import com.google.code.twig.Registry;
import com.google.code.twig.Settings;
import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.translator.ChainedTranslator;
import com.google.code.twig.translator.FieldTranslator;
import com.google.code.twig.translator.PolymorphicTranslator;
import com.google.code.twig.util.EntityToKeyFunction;
import com.google.code.twig.util.Reflection;
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
 * stuff on top. Translators are very specific to coding object fields.
 *
 * @author John Patterson (jdpatterson@gmail.com)
 */
/**
 * @author John Patterson
 *
 */
/**
 * @author John Patterson
 *
 */
/**
 * @author John Patterson
 *
 */
/**
 * @author John Patterson
 *
 */
public abstract class TranslatorObjectDatastore extends BaseObjectDatastore
{
	// keeps track of which instances are associated with which keys
	protected final InstanceKeyCache keyCache = new InstanceKeyCache();

	// ensure only used be a single thread
	protected Thread thread;

	/** are properties indexed by default */
	protected boolean indexed;

	// translators are selected for particular fields by the configuration
	private final ObjectFieldTranslator objectFieldTranslator;
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
	protected KeySpecification encodeKeySpec;

	// the key of the currently decoding entity
	protected Key decodeKey;

	// current activation depth
	final int defaultActivationDepth = Integer.MAX_VALUE;

	// indicates we are only associating instances so do not store them
	boolean associating = false;

	// when we associating should instances be activated
	boolean activate = true;

	protected Object refresh;
	
	boolean denormalising;
	
	StandardCommand command;

	Map<String, Iterator<Key>> allocatedIdRanges;

	private final Configuration configuration;

	public TranslatorObjectDatastore(Settings settings, Configuration configuration, Registry registry)
	{
		super(settings);
		this.configuration = configuration;

		TypeConverter converter = getTypeConverter();

		this.thread = Thread.currentThread();

		// top level translator which examines object field values
		objectFieldTranslator = new ObjectFieldTranslator(converter);

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
		embedTranslator.append(new CollectionTranslator(this, embedTranslator));
		embedTranslator.append(new MapTranslator(this, embedTranslator, converter));
		embedTranslator.append(objectFieldTranslator);

		// if the property has a key then the model was changed
		// from a reference to embedded so decode the reference
		embedTranslator.append(independantTranslator);

		// polymorphic translator - try simple values and then embedded components
		ChainedTranslator polymorphicValueTranslator = new ChainedTranslator(valueTranslatorChain, embedTranslator);
		PropertyTranslator polymorphicTranslator = new PolymorphicTranslator(polymorphicValueTranslator, configuration);

		// allow it to handle maps and lists of polymorphic instances
		polymorphicComponentTranslator = new ChainedTranslator(
				new CollectionTranslator(this, polymorphicTranslator),
				new MapTranslator(this, polymorphicTranslator, converter),
				polymorphicTranslator);

		// by default, translate simple values and then try the fallback if that fails
		defaultTranslator = new ChainedTranslator();
		defaultTranslator.append(new CollectionTranslator(this, defaultTranslator));
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
		return new StandardStoreCommand(this);
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

	// TODO hook these up!
//	 protected void onAfterStore(Object instance, Key key)
//	 {
//	 }
//
//	 protected void onBeforeStore(Object instance)
//	 {
//	 }
//
//	 protected void onBeforeLoad(Key key)
//	 {
//	 }
//
//	 protected void onAfterLoad(Key key, Object instance)
//	 {
//	 }

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
	public final void update(Object instance)
	{
		assert instance != null;

		// store but set the internal update flag so
		store().update(true).instance(instance).now();
	}

	@Override
	public void updateAll(Collection<?> instances)
	{
		assert instances != null;

		store().update(true).instances(instances).now();
	}

	@Override
	public final void storeOrUpdate(Object instance)
	{
		assert instance != null;
		store().instance(instance).now();
	}

	@Override
	public void storeOrUpdateAll(Collection<?> instances)
	{
		assert instances != null;
		store().instances(instances).now();
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
		T existing = keyCache.getInstance(key);
		if (existing != null)
		{
			return existing;
		}
		keyCache.cache(key, instance, version > 0, version);
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
		return associate(instance, activated, null);
	}

	@Override
	public <T> T associate(Class<T> type, long id)
	{
		return createAndAssociate(type, id);
	}

	@Override
	public <T> T associate(Class<T> type, String id)
	{
		return createAndAssociate(type, id);
	}

	private <T> T createAndAssociate(Class<T> type, Object id)
	{
		@SuppressWarnings("unchecked")
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

	@Override @Deprecated
	// TODO separate association from storing
	public <T> T associate(T instance, boolean activate, Object parent)
	{
		return associate(instance, activate, parent, null);
	}

	@Override
	// TODO separate association from storing
	public <T> T associate(T instance, boolean activate, Object parent, Object id)
	{
		return associate(instance, activate, parent, id, 0);
	}
	
	public <T> T associate(T instance, boolean activate, Object parent, Object id, long version)
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
			this.activate = activate;

			// use a store command to analyse this and all referenced instances
			StandardSingleStoreCommand<Object> command = store().instance(null);
			if (parent != null)
			{
				command.parent(parent);
			}

			// create key from @Id and @Parent fields
			Key key = command.instanceToKey(instance, id);

			// check if there is already an instance with this key
			T existing = keyCache.getInstance(key);

			if (existing != null)
			{
				return existing;
			}
			else
			{
				keyCache.cache(key, instance, activate, version);
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

			// instance may have been garbages collected
			if (instance != null)
			{
				StandardCommonStoreCommand.setInstanceId(instance, key, this);
				StandardCommonStoreCommand.setInstanceKey(instance, key, this);
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

	protected final InstanceKeyCache getKeyCache()
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

		List<Key> keys = new ArrayList<Key>(instances.size());
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
		Map<Key, Object> loaded = load().keys(keys).instances(instances.iterator()).now();

		if (loaded.size() != keys.size())
		{
			// TODO throw more specific runtime exception with missing keys
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

	// TODO this does not feel right - its internal
	protected boolean isAssociating()
	{
		return this.associating;
	}

	protected final void setIndexed(boolean indexed)
	{
		this.indexed = indexed;
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

	protected void deleteKeys(Collection<Key> keys)
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
			return TranslatorObjectDatastore.this.configuration.index(field);
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
			}
			else
			{
				assert clazz.isInstance(refresh);
				result = refresh;
			}
			
			// only cache persistent instance - not embedded components
			if (keyCache.getInstance(decodeKey) == null)
			{
				// we have not activated the instance yet
				keyCache.cache(decodeKey, result, null, 0);
			}

			return result;
		}

		@Override
		protected void decodeField(Object instance, Field field, Path path, Set<Property> properties)
		{
			// temporarily change the activation depth if this field has one set
			StandardDecodeCommand<?> decode = (StandardDecodeCommand<?>) command;
			int depth = configuration.activationDepth(field, decode.getCurrentActivationDepth());
			decode.setCurrentActivationDepth(depth);

			// when denormalising we enhance the existing value
			if (denormalising)
			{
				refresh = Reflection.get(field, instance);
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
	protected Object createInstance(Class<?> clazz)
	{
		return objectFieldTranslator.defautCreateInstance(clazz);
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
}
