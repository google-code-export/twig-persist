package com.vercer.engine.persist;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.conversion.TypeConverters;
import com.vercer.engine.persist.strategy.NamingStrategy;
import com.vercer.engine.persist.strategy.RelationshipStrategy;
import com.vercer.engine.persist.strategy.StorageStrategy;
import com.vercer.engine.persist.translator.ChainedTranslator;
import com.vercer.engine.persist.translator.CollectionTranslator;
import com.vercer.engine.persist.translator.CommonTypesTranslator;
import com.vercer.engine.persist.translator.DecoratingTranslator;
import com.vercer.engine.persist.translator.DelayedTranslator;
import com.vercer.engine.persist.translator.EntityTranslator;
import com.vercer.engine.persist.translator.EnumTranslator;
import com.vercer.engine.persist.translator.KeyCachingTranslator;
import com.vercer.engine.persist.translator.NativeDirectTranslator;
import com.vercer.engine.persist.translator.ObjectFieldTranslator;
import com.vercer.engine.persist.util.SimpleProperty;
import com.vercer.util.reference.ObjectReference;
import com.vercer.util.reference.ReadOnlyObjectReference;
import com.vercer.util.reference.SimpleObjectReference;

public class StrategyTypesafeSession extends TranslatorTypesafeSession implements
		CachingTypesafeSession
{
	private final NamingStrategy naming;
	private static final Logger LOG = Logger.getLogger(StrategyTypesafeSession.class.getCanonicalName());

	// state fields
	private KeySpecification writeKeySpec;
	private Key readKey;

	protected final PropertyTranslator componentTranslator;
	protected final PropertyTranslator parentTranslator;
	protected final PropertyTranslator independantTranslator;
	protected final PropertyTranslator keyFieldTranslator;
	protected final PropertyTranslator childTranslator;
	protected final PropertyTranslator valueTranslator;
	protected final PropertyTranslator defaultTranslator;
	protected final KeyCachingTranslator keyCachingTranslator;

	public StrategyTypesafeSession(DatastoreService datastore,
			final RelationshipStrategy relationships, final StorageStrategy storage,
			final NamingStrategy fields, final TypeConverters converters)
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
				return storage.typeOf(field);
			}

			@Override
			protected String fieldName(Field field)
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
					return componentTranslator;
				}
				else
				{
					return defaultTranslator;
				}
			}

			@Override
			protected void onInstanceCreated(Object instance)
			{
				// need to cache the instance immediately so children can ref it
				keyCachingTranslator.cache(readKey, instance);
			}

		};

		valueTranslator = createValueTranslator();

		// the last option is to create a new entity and reference it by a key
		keyCachingTranslator = new KeyCachingTranslator(new EntityTranslator(this));

		parentTranslator = new ParentEntityTranslator(keyCachingTranslator);
		independantTranslator = new CollectionTranslator(new UnrelatedEntityTranslator(keyCachingTranslator));
		keyFieldTranslator = new KeyFieldTranslator(valueTranslator, converters);
		childTranslator = new CollectionTranslator(new DelayedTranslator(keyCachingTranslator));
		componentTranslator = new CollectionTranslator(translator);
		defaultTranslator = createDefaultTranslator();

		setPropertyTranslator(translator);
	}

	protected ChainedTranslator createDefaultTranslator()
	{
		return new ChainedTranslator(valueTranslator, independantTranslator);
	}

	protected ChainedTranslator createValueTranslator()
	{
		ChainedTranslator result = new ChainedTranslator();
		result.append(new NativeDirectTranslator());
		result.append(new CommonTypesTranslator());
		result.append(new EnumTranslator());

		return result;
	}

	@Override
	public Object encode(Object object)
	{
		// the main translator will <b>always</b> try to read fields first so
		// must use value translator
		Set<Property> properties = defaultTranslator.typesafeToProperties(object, Path.EMPTY_PATH, true);
		return properties.iterator().next().getValue();
	}

	@Override
	protected String typeToKind(Type type)
	{
		String kind = naming.typeToKind(type);
		writeKeySpec.setKind(kind);
		return kind;
	}

	@Override
	protected Type kindToType(String kind)
	{
		return naming.kindToType(kind);
	}

	@Override
	protected void onBeforeSave(Object instance)
	{
		KeySpecification keySpec = new KeySpecification();

		// TODO this should use a ref to teh cache and maybe use
		// a different way to know we are a child
		// perhaps child translator should set parent key ref
		// which would mean writeKeySpec must already exist... hmmmmm

		// an existing write key spec indicates that we are a child
		if (writeKeySpec != null)
		{
			keySpec.setParentKeyReference(writeKeySpec.toObjectReference());
		}

		keyCachingTranslator.cacheKeyReferenceForInstance(instance, keySpec.toObjectReference());

		writeKeySpec = keySpec;
	}

	@Override
	protected void onAfterStore(Object instance, Key key)
	{
		writeKeySpec = null;
	}

	@Override
	protected void onBeforeRestore(Entity entity)
	{
		readKey = entity.getKey();
	}

	@Override
	protected void onAfterRestore(Entity entity, Object instance)
	{
		readKey = null;
	}

	@Override
	protected Entity createEntity(KeySpecification specification)
	{
		// add any key info we have gathered while writing the fields
		specification.merge(writeKeySpec);
		return super.createEntity(specification);
	}

	protected boolean propertiesIndexedByDefault()
	{
		return true;
	}

	protected void configure(ChainedTranslator chained)
	{
	}

	public final void clearKeyCache()
	{
		keyCachingTranslator.clearKeyCache();
	}

	public final Key evictEntity(Object reference)
	{
		return keyCachingTranslator.evictEntity(reference);
	}

	public final Object evictKey(Key key)
	{
		return keyCachingTranslator.evictKey(key);
	}

	public void cache(Key key, Object instance)
	{
		keyCachingTranslator.cache(key, instance);
	}

	protected <T> ObjectReference<T> createStateReference()
	{
		return new SimpleObjectReference<T>();
	}

	@Override
	protected RuntimeException exceptionOnTranslateWrite(RuntimeException e, Object instance)
	{
		String message = "There was a problem translating instance " + instance + "\n";
		message += "Make sure instances are either Serializable or configured as components or entities.";
		return new IllegalStateException(message, e);
	}

	private final class KeyFieldTranslator extends DecoratingTranslator
	{
		private final TypeConverters converters;

		private KeyFieldTranslator(PropertyTranslator chained, TypeConverters converters)
		{
			super(chained);
			this.converters = converters;
		}

		public Set<Property> typesafeToProperties(Object keyObject, Path prefix, boolean indexed)
		{
			// the key name is not stored in the fields but only in key
			// TODO might be able to use long directly also
			String keyName = converters.convert(keyObject, String.class);
			writeKeySpec.setName(keyName);
			return Collections.emptySet();
		}

		public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
		{
			// the key value is not stored in the properties but in the key
			String keyName = readKey.getName();
			Object keyObject = converters.convert(keyName, type);
			return keyObject;
		}
	}

	private final class ParentEntityTranslator implements PropertyTranslator
	{
		private final PropertyTranslator chained;

		private ParentEntityTranslator(PropertyTranslator chained)
		{
			this.chained = chained;
		}

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

			Property property = new SimpleProperty(prefix, parentKey, true);
			properties = Collections.singleton(property);

			// pass the key to normal cache/reading chain
			return chained.propertiesToTypesafe(properties, prefix, type);
		}

		public Set<Property> typesafeToProperties(final Object object, final Path prefix, final boolean indexed)
		{
			// the parent key is not stored as properties but inside the key
			ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
			{
				public Key get()
				{
					// clear the current key spec so the parent is not also child
					KeySpecification current = writeKeySpec;
					writeKeySpec = null;

					Set<Property> properties = chained.typesafeToProperties(object, prefix, indexed);
					writeKeySpec = current;

					return (Key) properties.iterator().next().getValue();
				}
			};

			// an existing parent key ref shows parent is still being stored
			if (writeKeySpec.getParentKeyReference() == null)
			{
				// store the parent key inside the current key
				writeKeySpec.setParentKeyReference(keyReference);
			}

			// no fields are stored for parent
			return Collections.emptySet();
		}
	}

	private final class UnrelatedEntityTranslator extends DecoratingTranslator
	{
		public UnrelatedEntityTranslator(PropertyTranslator chained)
		{
			super(chained);
		}

		public Object propertiesToTypesafe(Set<Property> properties, Path prefix, Type type)
		{
			return chained.propertiesToTypesafe(properties, prefix, type);
		}

		public Set<Property> typesafeToProperties(final Object object, final Path path, final boolean indexed)
		{

			ReadOnlyObjectReference<Key> keyReference = new ReadOnlyObjectReference<Key>()
			{
				public Key get()
				{
					// clear the current key spec so it is not used as parent
					KeySpecification current = writeKeySpec;
					writeKeySpec = null;

					Set<Property> properties = chained.typesafeToProperties(object, path, indexed);

					// replace it to continue processing potential children
					writeKeySpec = current;

					return (Key) properties.iterator().next().getValue();
				}
			};

			Property property = new SimpleProperty(path, keyReference, indexed);
			return Collections.singleton(property);
		}
	}
}
