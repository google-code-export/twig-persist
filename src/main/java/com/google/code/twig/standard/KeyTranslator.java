package com.google.code.twig.standard;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Set;

import com.google.appengine.api.datastore.KeyFactory;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;

/**
 * Translator to encode/decode fields with a GAE Key
 * (com.google.appengine.api.datastore.Key). The field with the GAE Key can
 * either be of the complex type Key or the String encoded Key. The translator
 * will determine which to use at runtime. Simple annotate the field with @GaeKey
 * 
 * Example usage:
 * 
 * @GaeKey protected String myKey;
 * 
 *         or
 * 
 * @GaeKey protected Key myKey;
 * 
 * @author Brad Seefeld
 */
public class KeyTranslator implements PropertyTranslator
{

	protected TranslatorObjectDatastore datastore;

	public KeyTranslator(TranslatorObjectDatastore datastore)
	{
		this.datastore = datastore;
	}

	@Override
	public Object decode(Set<Property> properties, Path path, Type type)
	{
		// GAE encoded Key as String
		if (type == String.class)
		{
			return KeyFactory.keyToString(datastore.decodeKey);
		}
		else
		{
			// The type must be a Key...
			return datastore.decodeKey;
		}
	}

	/**
	 * No need to encode. Keys are saved automatically by the datastore.
	 */
	@Override
	public Set<Property> encode(Object instance, Path path, boolean indexed)
	{
		return Collections.emptySet();
	}
}
