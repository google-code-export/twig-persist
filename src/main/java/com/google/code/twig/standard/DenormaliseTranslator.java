package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.util.PropertySets;
import com.google.code.twig.util.collections.MergeSet;
import com.google.code.twig.util.generic.Generics;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

public class DenormaliseTranslator implements PropertyTranslator
{
	protected final PropertyTranslator relation;
	protected final Set<String> paths;
	protected final TranslatorObjectDatastore datastore;

	public DenormaliseTranslator(TranslatorObjectDatastore datastore, PropertyTranslator relation, Set<String> paths)
	{
		this.datastore = datastore;
		this.relation = relation;
		this.paths = paths;
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		Object value = null;
		Set<Property> relationProperty;
		if (properties.isEmpty())
		{
			relationProperty = Collections.emptySet();
		}
		else
		{
			Property property = PropertySets.firstProperty(properties);
			value = property.getValue();

			// a referenced key property will have same path
			if (property.getPath().equals(path))
			{
				// first property will hold key(s)
				relationProperty = PropertySets.singletonPropertySet(path, value, false);
			}
			else
			{
				// no key property with a parent relation
				relationProperty = Collections.emptySet();
			}
		}

		// we can have a collection of keys
		Object decoded = relation.decode(relationProperty, path, type);
		if (decoded == PropertyTranslator.NULL_VALUE)
		{
			return NULL_VALUE;
		}

		if (Collection.class.isAssignableFrom(Generics.erase(type)))
		{
			Collection<Object> instances = (Collection<Object>) decoded;

			// get all properties for each instance
			Iterator<Set<Property>> propertySetorator = IterableTranslator.decodePropertySets(properties).iterator();

			Type elementType = Generics.getTypeParameter(type, Collection.class.getTypeParameters()[0]);

			// enhance each instance with the extra properties
			for (Object instance : instances)
			{
				Set<Property> propertySet = propertySetorator.next();
				decodeDenormalisedFields(propertySet, path, elementType, instance);
			}
			return instances;
		}
		else
		{
			decodeDenormalisedFields(properties, path, type, decoded);
			return decoded;
		}
	}

	private void decodeDenormalisedFields(Set<Property> properties, Path path, Type type, Object instance)
	{
		// only decode embedded fields for unactivated instances
		if (!datastore.isActivated(instance) && !properties.isEmpty())
		{
			// TODO centralise all this state management to DecodeInstanceState
			// which contains DecodeObjectState

			Key existingDecodeKey = datastore.decodeKey;
			boolean existingDenormalising = datastore.denormalising;

			// do not create a new instance when decoding fields
			datastore.decodeKey = datastore.associatedKey(instance);
			datastore.denormalising = true;
			datastore.refresh = instance;

			// set field values for only the properties we denormalised
			Object embedded = datastore.getEmbeddedTranslator().decode(properties, path, type);

			assert instance == embedded;

			// replace the previous state
			datastore.decodeKey = existingDecodeKey;
			datastore.denormalising = existingDenormalising;
			datastore.refresh = null;
		}
	}

	@Override
	public Set<Property> encode(Object instance, final Path path, boolean indexed)
	{
		MergeSet<Property> result = new MergeSet<Property>(2);
		Set<Property> related = relation.encode(instance, path, indexed);
		result.addAll(related);

		KeyDetails existingEncodeKey = datastore.encodeKeyDetails;

		// make a dummy encode key because we already have the full key
		datastore.encodeKeyDetails = new KeyDetails();

		// get all embedded properties
		Set<Property> embedded = datastore.getEmbeddedTranslator().encode(instance, path, indexed);

		// reset the existing encode key
		datastore.encodeKeyDetails = existingEncodeKey;

		// filter all embedded properties to only store the ones we want
		Set<Property> filtered = Sets.filter(embedded, new Predicate<Property>()
		{
			@Override
			public boolean apply(Property input)
			{
				// get a relative path for filtering
				int size = path.getParts().size();
				Path relativePath = input.getPath().tail(size);
				if (Path.EMPTY_PATH.equals(relativePath))
				{
					// this path is the key(s) so keep it
					return true;
				}
				return filter(relativePath, input);
			}

		});

		result.addAll(filtered);

		return result;
	}

	protected boolean filter(Path path, Property input)
	{
		// empty field set signifies that all paths are to be included
		return paths.isEmpty() || paths.contains(path.toString().substring(1));
	}

	public Set<String> getPaths()
	{
		return this.paths;
	}

}
