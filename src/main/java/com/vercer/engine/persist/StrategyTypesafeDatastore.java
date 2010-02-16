package com.vercer.engine.persist;

import java.io.NotSerializableException;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;
import com.vercer.engine.persist.conversion.DefaultTypeConverter;
import com.vercer.engine.persist.conversion.TypeConverter;
import com.vercer.engine.persist.strategy.FieldTypeStrategy;
import com.vercer.engine.persist.strategy.RelationshipStrategy;
import com.vercer.engine.persist.strategy.StorageStrategy;
import com.vercer.engine.persist.translator.ChainedTranslator;
import com.vercer.engine.persist.translator.CoreTypesTranslator;
import com.vercer.engine.persist.translator.DecoratingTranslator;
import com.vercer.engine.persist.translator.EnumTranslator;
import com.vercer.engine.persist.translator.ListTranslator;
import com.vercer.engine.persist.translator.NativeDirectTranslator;
import com.vercer.engine.persist.translator.ObjectFieldTranslator;
import com.vercer.engine.persist.translator.PolymorphicTranslator;
import com.vercer.engine.persist.util.KeyCache;
import com.vercer.engine.persist.util.PropertySets;
import com.vercer.engine.persist.util.SinglePropertySet;
import com.vercer.util.reference.ReadOnlyObjectReference;

/**
 * Stateful layer responsible for caching key-object references and 
 * creating a PropertyTranslator that can be configured using Strategy 
 * instances.
 * 
 * @author John Patterson <john@vercer.com>
 */
public class StrategyTypesafeDatastore extends TranslatorTypesafeDatastore
{
	final FieldTypeStrategy fields;
	private static final Logger logger = Logger.getLogger(StrategyTypesafeDatastore.class.getName());

	// state fields
	private KeySpecification writeKeySpec;
	private Key readKey;

	private final PropertyTranslator componentTranslator;
	private final PropertyTranslator polyMorphicComponentTranslator;
	private final PropertyTranslator parentTranslator;
	private final PropertyTranslator independantTranslator;
	private final PropertyTranslator keyFieldTranslator;
	private final PropertyTranslator childTranslator;
	private final ChainedTranslator valueTranslator;
	private final PropertyTranslator defaultTranslator;
	private final KeyCache keyCache;

	/**
	 * Flag that indicates we are associating instances with this session so do not store them
	 */
	private boolean associating;
	
	private boolean batching;
	private List<Entity> batched;

	private int depth;
	private TypeConverter converter;
	private Object refreshing;

	public StrategyTypesafeDatastore(
			DatastoreService datastore,
			final RelationshipStrategy relationships,
			final StorageStrategy storage,
			final FieldTypeStrategy fields)
	{
		// use the protected constructor so we can configure the translator
		super(datastore);

		this.fields = fields;

		converter = createTypeConverter();
		
		// central translator that reads fields and delegates to the others
		PropertyTranslator translator = new ObjectFieldTranslator(converter)
		{
			@Override
			protected boolean indexed(Field field)
			{
				return storage.indexed(field);
			}

			@Override
			protected boolean stored(Field field)
			{
				return storage.stored(field);
			}

			@Override
			protected Type typeFromField(Field field)
			{
				return fields.typeOf(field);
			}

			@Override
			protected String fieldToPartName(Field field)
			{
				return fields.name(field);
			}

			@Override
			protected PropertyTranslator translator(Field field)
			{
				if (storage.entity(field))
				{
					if (relationships.parent(field))
					{
						return parentTranslator;
					}
					else if (relationships.child(field))
					{
						return childTranslator;
					}
					else
					{
						return independantTranslator;
					}
				}
				else if (relationships.key(field))
				{
					return keyFieldTranslator;
				}
				else if (storage.component(field))
				{
					if (storage.polymorphic(field))
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

			@Override
			// TODO remove this when using ObjectRefernece<Object> instead
			protected Object createInstance(Class<?> clazz)
			{
				Object instance;
				if (refreshing == null)
				{
					instance = super.createInstance(clazz);
				}
				else
				{
					// replace fields with new ones
					instance = refreshing;
					refreshing = null;
				}

				// need to cache the instance immediately so children can ref it
				if (keyCache.getCachedEntity(readKey) == null)
				{
					// only cache first time - not for embedded components
					keyCache.cache(readKey, instance);
				}

				return instance;
			}

		};

		valueTranslator = createValueTranslatorChain();

		parentTranslator = new ParentEntityTranslator();
		independantTranslator = new ListTranslator(new IndependantEntityTranslator());
		keyFieldTranslator = new KeyFieldTranslator(valueTranslator, converter);
		childTranslator = new ListTranslator(new ChildEntityTranslator());
		componentTranslator = new ListTranslator(translator);
		polyMorphicComponentTranslator = new ListTranslator(new PolymorphicTranslator(translator));
		defaultTranslator = new ChainedTranslator(new ListTranslator(valueTranslator), getFallbackTranslator());

		setPropertyTranslator(translator);
		keyCache = new KeyCache();
	}

	/**
	 * @return The translator which is used if no others are configured
	 */
	protected PropertyTranslator getFallbackTranslator()
	{
		return independantTranslator;
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
		result.append(new CoreTypesTranslator());
		result.append(new EnumTranslator());
		return result;
	}

	@Override
	public final Object encode(Object object)
	{
		// the main translator will always try to read fields first so must use value translator
		Set<Property> properties = getDefaultTranslator().typesafeToProperties(object, Path.EMPTY_PATH, true);
		return properties.iterator().next().getValue();
	}

	@Override
	public String typeToKind(Type type)
	{
		return fields.typeToKind(type);
	}

	@Override
	protected Type kindToType(String kind)
	{
		return fields.kindToType(kind);
	}

	@Override
	protected void onBeforeStore(Object instance)
	{
		depth++;
		if (keyCache.getCachedKey(instance) != null)
		{
			throw new IllegalStateException("Cannot store same instance twice: " + instance);
		}

		KeySpecification keySpec = new KeySpecification();

		// an existing write key spec indicates that we are a child
		if (writeKeySpec != null)
		{
			keySpec.setParentKeyReference(writeKeySpec.toObjectReference());
		}

		keyCache.cacheKeyReferenceForInstance(instance, keySpec.toObjectReference());

		writeKeySpec = keySpec;
	}

	@Override
	protected void onAfterStore(Object instance, Entity entity)
	{
		depth--;
		writeKeySpec = null;
		keyCache.cache(entity.getKey(), instance);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T toTypesafe(Entity entity, Predicate<String> propertyPredicate)
	{
		// cast needed to avoid sun generics bug "no unique maximal instance exists..."
		T typesafe = (T) keyCache.getCachedEntity(entity.getKey());
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
	protected final <T> T keyToInstance(Key key, Predicate<String> propertyPredicate)
	{
		// only cache full instances
		T typesafe = null;
		if (propertyPredicate == null)
		{
			typesafe = (T) keyCache.getCachedEntity(key);
		}
		
		if (typesafe == null)
		{
			typesafe = (T) super.keyToInstance(key, propertyPredicate);
		}
		
		return typesafe;
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
	protected Key putEntityToDatstore(Entity entity)
	{
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
			if (batched == null)
			{
				batched = new ArrayList<Entity>();
			}
			batched.add(entity);

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
			// actually save the entity
			return super.putEntityToDatstore(entity);
		}
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
		String converted = converter.convert(key, String.class);
		Key parentKey = null;
		if (parent != null)
		{
			parentKey = keyCache.getCachedKey(parent);
		}
		return internalLoad(type, converted, parentKey);
	}

	public final <T> Iterator<T> find(Class<T> type, Object parent)
	{
		return find(type, parent, (FindOptions) null);
	}

	public final <T> Iterator<T> find(Class<T> type, Object parent, FindOptions options)
	{
		return find(type, keyCache.getCachedKey(parent), options);
	}

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
		Key evicted= keyCache.evictInstance(instance);
		deleteKeys(Collections.singleton(evicted));
	}

	public final void deleteAll(Collection<?> instances)
	{
		deleteKeys(Collections2.transform(instances, instanceToKey));
		for (Object instance : instances)
		{
			keyCache.evictInstance(instance);
		}
	}
	
	private Object getInstanceFromCacheOrLoad(Key key)
	{
		Object instance = keyCache.getCachedEntity(key);
		if (instance == null)
		{
			instance = load(key);
			assert instance != null;
		}
		return instance;
	}

	private Key getKeyFromCacheOrStore(final Object instance)
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
		Key parentKey = keyCache.getCachedKey(parent);
		return store(instance, parentKey, name);
	}

	public final Key associatedKey(Object instance)
	{
		return keyCache.getCachedKey(instance);
	}

	public final List<Key> storeAll(Collection<?> instances)
	{
		return storeAll(instances, (Key) null);
	}

	public final List<Key> storeAll(Collection<?> instances, Object parent)
	{
		Key parentKey = null;
		if (parent != null)
		{
			parentKey = keyCache.getCachedKey(parent);
		}
		batching = true;
		for (Object instance : instances)
		{
			store(instance, parentKey);
		}
		batching = false;
		
		// TODO keys will also contain related entities - is this correct?
		List<Key> put = getService().put(batched);
		batched.clear();
		return put;
	}
	
	public void refresh(Object instance)
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

	protected final PropertyTranslator getComponentTranslator()
	{
		return componentTranslator;
	}

	public final PropertyTranslator getKeyFieldTranslator()
	{
		return keyFieldTranslator;
	}

	public final PropertyTranslator getDefaultTranslator()
	{
		return defaultTranslator;
	}
	

	private final Function<Object, Key> instanceToKey = new Function<Object, Key>()
	{
		public Key apply(Object instance)
		{
			return keyCache.getCachedKey(instance);
		}
	};

	private final class KeyFieldTranslator extends DecoratingTranslator
	{
		private final TypeConverter converters;

		private KeyFieldTranslator(PropertyTranslator chained, TypeConverter converters)
		{
			super(chained);
			this.converters = converters;
		}

		public Set<Property> typesafeToProperties(Object instance, Path path, boolean indexed)
		{
			assert path.getParts().size() == 1 : "Key field should be in root Entity";
			
			// key spec may be null if we are in an update as we already have the key
			if (writeKeySpec != null)
			{
				if (instance != null)
				{
					// treat 0 the same as null
					if (!instance.equals(0))
					{
						// the key name is not stored in the fields but only in key
						String keyName = converters.convert(instance, String.class);
						writeKeySpec.setName(keyName);
					}
				}
			}
			return Collections.emptySet();
		}

		public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
		{
			assert properties.isEmpty();

			// the key value is not stored in the properties but in the key
			Object keyValue = readKey.getName();
			if (keyValue == null)
			{
				keyValue = readKey.getId();
			}
			Object keyObject = converters.convert(keyValue, type);
			return keyObject;
		}
	}

	private final class ParentEntityTranslator implements PropertyTranslator
	{
		public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
		{
			// properties are not used as the parent is found by the key
			assert properties.isEmpty();

			// put the key in a property
			Key parentKey = readKey.getParent();

			if (parentKey == null)
			{
				throw new IllegalStateException("No parent for key: " + readKey);
			}

			return getInstanceFromCacheOrLoad(parentKey);
		}

		public Set<Property> typesafeToProperties(final Object instance, final Path prefix, final boolean indexed)
		{
			// the parent key is not stored as properties but inside the key
			ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
			{
				public Key get()
				{
					// clear the current key spec so the parent is not also child
					KeySpecification current = writeKeySpec;
					writeKeySpec = null;

					Key key = getKeyFromCacheOrStore(instance);

					writeKeySpec = current;

					return key;
				}
			};

			// an existing parent key ref shows parent is still being stored
			if (writeKeySpec != null && writeKeySpec.getParentKeyReference() == null)
			{
				// store the parent key inside the current key
				writeKeySpec.setParentKeyReference(keyReference);
			}

			// no fields are stored for parent
			return Collections.emptySet();
		}
	}

	private final class ChildEntityTranslator implements PropertyTranslator
	{
		public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
		{
			Key key = PropertySets.firstValue(properties);
			return getInstanceFromCacheOrLoad(key);
		}

		public Set<Property> typesafeToProperties(final Object instance, final Path path, final boolean indexed)
		{
			ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
			{
				public Key get()
				{
					// clear the current key spec so it is not used as parent
					KeySpecification current = writeKeySpec;

					Key key = keyCache.getCachedKey(instance);
					if (key == null)
					{
						key = store(instance);
					}

					// replace it to continue processing potential children
					writeKeySpec = current;

					return key;
				}
			};

			return new SinglePropertySet(path, keyReference, indexed);
		}
	}

	private final class IndependantEntityTranslator implements PropertyTranslator
	{
		public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
		{
			Key key = PropertySets.firstValue(properties);
			return getInstanceFromCacheOrLoad(key);
		}

		public Set<Property> typesafeToProperties(final Object instance, final Path path, final boolean indexed)
		{
			assert instance != null;
			ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
			{
				public Key get()
				{
					// clear the current key spec so it is not used as parent
					KeySpecification current = writeKeySpec;
					writeKeySpec = null;

					Key key = getKeyFromCacheOrStore(instance);

					// replace it to continue processing potential children
					writeKeySpec = current;

					return key;
				}
			};

			return new SinglePropertySet(path, keyReference, indexed);
		}
	}

}
