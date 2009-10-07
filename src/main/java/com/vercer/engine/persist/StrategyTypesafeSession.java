package com.vercer.engine.persist;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
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

public class StrategyTypesafeSession extends TranslatorTypesafeDatastore implements TypesafeSession
{
	final FieldTypeStrategy naming;
//	private static final Logger LOG = Logger.getLogger(StrategyTypesafeSession.class.getName());

	// state fields
	private KeySpecification writeKeySpec;
	private Key readKey;

	protected final PropertyTranslator componentTranslator;
	protected final PropertyTranslator polyMorphicComponentTranslator;
	protected final PropertyTranslator parentTranslator;
	protected final PropertyTranslator independantTranslator;
	protected final PropertyTranslator keyFieldTranslator;
	protected final PropertyTranslator childTranslator;
	protected final PropertyTranslator valueTranslator;
	protected final PropertyTranslator defaultTranslator;
	protected final KeyCache keyCache;

	/**
	 * Flag that indicates we are associating instances with this session so do not store them
	 */
	private boolean associating;

	public StrategyTypesafeSession(
			DatastoreService datastore,
			final RelationshipStrategy relationships,
			final StorageStrategy storage,
			final FieldTypeStrategy fields,
			final TypeConverter converters)
	{
		// use the protected constructor so we can configure the translator
		super(datastore);

		this.naming = fields;

		// central translator that reads fields and delegates to the other
		// translators
		PropertyTranslator translator = new ObjectFieldTranslator(converters)
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
					if (storage.polyMorphic(field))
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
				Object instance = super.createInstance(clazz);

				// need to cache the instance immediately so children can ref it
				if (keyCache.getCachedEntity(readKey) == null)
				{
					// only cache first time - not for embedded components
					keyCache.cache(readKey, instance);
				}

				return instance;
			}

		};

		valueTranslator = createValueTranslator();

		// the last option is to create a new entity and reference it by a key
		keyCache = new KeyCache();

		parentTranslator = new ParentEntityTranslator();
		independantTranslator = new ListTranslator(new IndependantEntityTranslator());
		keyFieldTranslator = new KeyFieldTranslator(valueTranslator, converters);
		childTranslator = new ListTranslator(new ChildEntityTranslator());
		componentTranslator = new ListTranslator(translator);
		polyMorphicComponentTranslator = new ListTranslator(new PolymorphicTranslator(translator));
		defaultTranslator = createDefaultTranslator();

		setPropertyTranslator(translator);
	}

	protected PropertyTranslator createDefaultTranslator()
	{
		return new ListTranslator(new ChainedTranslator(valueTranslator, independantTranslator));
	}

	protected ChainedTranslator createValueTranslator()
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
		Set<Property> properties = defaultTranslator.typesafeToProperties(object, Path.EMPTY_PATH, true);
		return properties.iterator().next().getValue();
	}

	@Override
	public String typeToKind(Type type)
	{
		return naming.typeToKind(type);
	}

	@Override
	protected Type kindToType(String kind)
	{
		return naming.kindToType(kind);
	}

	@Override
	protected void onBeforeStore(Object instance)
	{
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
		writeKeySpec = null;
		keyCache.cache(entity.getKey(), instance);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T toTypesafe(Entity entity)
	{
		// cast needed to avoid sun generics bug "no unique maximal instance exists..."
		T typesafe = (T) keyCache.getCachedEntity(entity.getKey());
		if (typesafe == null)
		{
			Key current = readKey;
			readKey = entity.getKey();
			// cast needed to avoid sun generics bug "no unique maximal instance exists..."
			typesafe = (T) super.toTypesafe(entity);
			readKey = current;
		}

		return typesafe;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T load(Key key)
	{
		T typesafe = (T) keyCache.getCachedEntity(key);
		if (typesafe == null)
		{
			typesafe = (T) super.load(key);
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
		keyCache.evictEntity(reference);
	}

	public final void associate(Object instance, Key key)
	{
		keyCache.cache(key, instance);
	}

	public final void associate(Object instance)
	{
		// convert the instance so
		associating = true;
		store(instance);
		associating = false;
	}

	@Override
	protected Key putEntityToDatstore(Entity entity)
	{
		if (associating == false)
		{
			// actually save the entity
			return super.putEntityToDatstore(entity);
		}
		else
		{
			// do not save the entity because we just want the key
			Key key = entity.getKey();
			if (!key.isComplete())
			{
				// incomplete keys are no good to us
				throw new IllegalArgumentException("Entity does not have complete key: " + entity);
			}
			return key;
		}
	}

	@Override
	protected RuntimeException exceptionOnTranslateWrite(Exception e, Object instance)
	{
		String message = "There was a problem translating instance " + instance + "\n";
		message += "Make sure instances are either Serializable or configured as components or entities.";
		return new IllegalStateException(message, e);
	}

	private final class KeyFieldTranslator extends DecoratingTranslator
	{
		private final TypeConverter converters;

		private KeyFieldTranslator(PropertyTranslator chained, TypeConverter converters)
		{
			super(chained);
			this.converters = converters;
		}

		public Set<Property> typesafeToProperties(Object instance, Path prefix, boolean indexed)
		{
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

	public final <T> T find(Class<T> type, Object parent, String name)
	{
		return find(type, keyCache.getCachedKey(parent), name);
	}

	public final <T> Iterator<T> find(Class<T> type, Object parent)
	{
		return find(type, keyCache.getCachedKey(parent));
	}

	public final void update(Object instance)
	{
		Key key = keyCache.getCachedKey(instance);
		if (key == null)
		{
			throw new IllegalArgumentException("Can only update entities in the session");
		}
		update(instance, key);
	}


	public final void delete(Object instance)
	{
		delete(keyCache.getCachedKey(instance));
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

	public Key store(Object instance, Object parent)
	{
		Key parentKey = keyCache.getCachedKey(parent);
		return store(instance, parentKey);
	}

	public Key store(Object instance, Object parent, String name)
	{
		Key parentKey = keyCache.getCachedKey(parent);
		return store(instance, parentKey, name);
	}
}
