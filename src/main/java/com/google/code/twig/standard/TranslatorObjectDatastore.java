package com.google.code.twig.standard;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyRange;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.annotation.Id;
import com.google.code.twig.configuration.Configuration;
import com.google.code.twig.conversion.CombinedConverter;
import com.google.code.twig.conversion.TypeConverter;
import com.google.code.twig.translator.ChainedTranslator;
import com.google.code.twig.translator.ListTranslator;
import com.google.code.twig.translator.MapTranslator;
import com.google.code.twig.translator.ObjectFieldTranslator;
import com.google.code.twig.translator.PolymorphicTranslator;
import com.google.code.twig.util.EntityToKeyFunction;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Iterators;
import com.vercer.util.Reflection;

/**
 * Stateful layer responsible for caching key-object references and creating a
 * PropertyTranslator that can be configured using Strategy instances.
 * 
 * @author John Patterson <john@vercer.com>
 */
public abstract class TranslatorObjectDatastore extends BaseObjectDatastore
{
	// keeps track of which instances are associated with which keys
	protected final InstanceKeyCache keyCache;
	
	// are properties indexed by default
	protected boolean indexed;

	// key details are updated as the current instance is encoded
	protected KeySpecification encodeKeySpec;

	// the key of the currently decoding entity
	protected Key decodeKey;

	// current activation depth
	int activationDepth = Integer.MAX_VALUE;
	
	// translators are selected for particular fields by a strategy
	private final PropertyTranslator objectFieldTranslator;
	private final PropertyTranslator embedTranslator;
	private final PropertyTranslator polyMorphicComponentTranslator;
	private final PropertyTranslator parentTranslator;
	private final PropertyTranslator independantTranslator;
	private final PropertyTranslator keyFieldTranslator;
	private final PropertyTranslator childTranslator;
	private final ChainedTranslator valueTranslatorChain;
	private final PropertyTranslator gaeKeyFieldTranslator;
	private final PropertyTranslator defaultTranslator;

	private static final Map<Class<?>, Field> idFields = new ConcurrentHashMap<Class<?>, Field>();

	// indicates we are only associating instances so do not store them
	boolean associating;

	// set when all entities should be collected and stored in one call
	Map<Object, Entity> batched;

	Map<String, Iterator<Key>> allocatedIdRanges;
	
	// set when an instance is to be refreshed rather than instantiated
	private Object refresh;

	private final CombinedConverter converter;

	private final Configuration configuration;

	public TranslatorObjectDatastore(Configuration configuration)
	{
		this.configuration = configuration;
		this.converter = createTypeConverter();
		
		// the main translator which converts to and from objects
		objectFieldTranslator = new StrategyObjectFieldTranslator(converter);

		valueTranslatorChain = createValueTranslatorChain();

		parentTranslator = new ParentRelationTranslator(this);
		independantTranslator = new RelationTranslator(this);
		gaeKeyFieldTranslator = new KeyTranslator(this);
		keyFieldTranslator = new IdFieldTranslator(this, valueTranslatorChain, converter);
		childTranslator = new ChildRelationTranslator(this);

		embedTranslator = new ListTranslator(new MapTranslator(objectFieldTranslator, converter));

		polyMorphicComponentTranslator = new ListTranslator(
				new MapTranslator(
						new PolymorphicTranslator(
								new ChainedTranslator(valueTranslatorChain, objectFieldTranslator), 
								configuration), 
						converter));

		defaultTranslator = new ListTranslator(
				new MapTranslator(
						new ChainedTranslator(valueTranslatorChain, getFallbackTranslator()),
						converter));

		keyCache = createKeyCache();
	}

	@Override
	public final StandardFindCommand find()
	{
		return new StandardFindCommand(this);
	}

	@Override
	public final StandardStoreCommand store()
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
	public <T> T load(Key key)
	{
		@SuppressWarnings("unchecked")
		T result = (T) load().key(key).now();
		return result;
	}

	@Override
	public final <T> T load(Class<? extends T> type, Object id)
	{
		return load().type(type).id(id).now();
	}

	@Override
	public final <I, T> Map<I, T> loadAll(Class<? extends T> type, Collection<? extends I> ids)
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

	@Override
	public final void refresh(Object instance)
	{
		assert instance != null;
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
		Object loaded = load().key(key).now();

		if (loaded == null)
		{
			throw new IllegalStateException("Instance to be refreshed could not be found");
		}
	}

	@Override
	public void refreshAll(Collection<?> instances)
	{
		assert instances != null;
		// TODO optimise! add to a stack then pop instances off the stack
		for (Object instance : instances)
		{
			refresh(instance);
		}
	}

	protected InstanceKeyCache createKeyCache()
	{
		return new InstanceKeyCache();
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
		if (configuration.entity(field))
		{
			PropertyTranslator translator;
			if (configuration.parent(field))
			{
				translator = parentTranslator;
			}
			else if (configuration.child(field))
			{
				translator = childTranslator;
			}
			else
			{
				translator = independantTranslator;
			}
			return translator;
		}
		else if (configuration.id(field))
		{
			return keyFieldTranslator;
		}
		else if (configuration.embed(field))
		{
			if (configuration.polymorphic(field))
			{
				return polyMorphicComponentTranslator;
			}
			else
			{
				return embedTranslator;
			}
		}
		else if (configuration.key(field)) {
			return gaeKeyFieldTranslator;
		}
		else
		{
			return defaultTranslator;
		}
	}

	/**
	 * @return The translator which is used if no others handle the instance
	 */
	protected abstract PropertyTranslator getFallbackTranslator();

	protected abstract CombinedConverter createTypeConverter();

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
	public final void associate(Object instance, Key key)
	{
		keyCache.cache(key, instance);
	}

	@Override
	public final void associate(Object instance)
	{
		// encode the instance so we can get its id and parent to make a key
		associating = true;
		store(instance);
		associating = false;
	}
	
	@Override
	public final void associate(Object instance, Object parent)
	{
		// encode the instance so we can get its id and parent to make a key
		associating = true;
		store().instance(instance).parent(parent).now();
		associating = false;
	}

	@Override
	public final void associateAll(Collection<?> instances)
	{
		// encode the instance so we can get its id and parent to make a key
		associating = true;
		storeAll(instances);
		associating = false;
	}

	@Override
	public final Key associatedKey(Object instance)
	{
		return keyCache.getKey(instance);
	}

	@Override
	public final int getActivationDepth()
	{
		return activationDepth;
	}

	@Override
	public final void setActivationDepth(int depth)
	{
		this.activationDepth = depth;
	}

	public CombinedConverter getConverter()
	{
		return converter;
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

	protected final PropertyTranslator getPolymorphicTranslator()
	{
		return polyMorphicComponentTranslator;
	}
	
	protected final PropertyTranslator getObjectFieldTranslator()
	{
		return objectFieldTranslator;
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

	final Query createQuery(Type type)
	{
		return new Query(configuration.typeToKind(type));
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

	protected void deleteKeys(Collection<Key> keys)
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
			return TranslatorObjectDatastore.this.configuration.index(field);
		}

		@Override
		protected boolean stored(Field field)
		{
			if (associating)
			{
				// if associating only decode fields required for the key
				return configuration.id(field) || configuration.parent(field);
			}
			else
			{
				return TranslatorObjectDatastore.this.configuration.store(field);
			}
		}

		@Override
		protected Type type(Field field)
		{
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

		@Override
		protected Object createInstance(Class<?> clazz)
		{
			// if we are refreshing an instance do not create a new one
			Object instance = refresh;
			if (instance == null)
			{
				instance = TranslatorObjectDatastore.this.createInstance(clazz);
				if (instance == null)
				{
					instance = super.createInstance(clazz);
				}
			}
			refresh = null;

			// cache the instance immediately so related instances can reference it
			if (keyCache.getInstance(decodeKey) == null)
			{
				// only cache first time - not for embedded components
				keyCache.cache(decodeKey, instance);
			}

			return instance;
		}

		@Override
		protected void decode(Object instance, Field field, Path path, Set<Property> properties)
		{
			int existingActivationDepth = activationDepth;
			try
			{
				activationDepth = configuration.activationDepth(field, activationDepth);
				super.decode(instance, field, path, properties);
			}
			finally
			{
				activationDepth = existingActivationDepth;
			}
		}

		@Override
		protected void onBeforeEncode(Path path, Object value)
		{
			if (!path.getParts().isEmpty())
			{
				// check that this embedded value is not a persistent instance
				if (keyCache.getKey(value) != null)
				{
					throw new IllegalStateException("Cannot embed persistent instance " + value + " at " + path );
				}
			}
		}

		@Override
		protected boolean isNullStored()
		{
			return TranslatorObjectDatastore.this.isNullStored();
		}
		
	}

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
				if (field.isAnnotationPresent(com.google.code.twig.annotation.Key.class)
						|| field.isAnnotationPresent(Id.class))
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

	/**
	 * Create a new instance which will have its fields populated from stored properties.
	 * 
	 * @param clazz The type to create
	 * @return A new instance or null to use the default behaviour
	 */
	protected Object createInstance(Class<?> clazz)
	{
		return null;
	}

	public Configuration getConfiguration()
	{
		return configuration;
	}

	protected abstract boolean isNullStored();

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

	private static final Function<Entity, Key> entityToKeyFunction = new EntityToKeyFunction();
}
