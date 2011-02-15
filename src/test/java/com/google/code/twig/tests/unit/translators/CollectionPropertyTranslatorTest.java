package com.google.code.twig.tests.unit.translators;


import static org.junit.Assert.assertEquals;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.code.twig.Path;
import com.google.code.twig.Property;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.conversion.PrimitiveConverter;
import com.google.code.twig.translator.DirectTranslator;
import com.google.code.twig.translator.ListTranslator;
import com.google.code.twig.util.SimpleProperty;

public class CollectionPropertyTranslatorTest
{
	private List<String> values;

	@Before
	public void setUp() throws Exception
	{
		values = new ArrayList<String>();
		values.add("john");
		values.add("is");
		values.add("cool");
	}

	@Test
	public void writeReadSimpleList() throws SecurityException, NoSuchFieldException
	{
		// create dummy translator that always returns a single property containing the instance
		PropertyTranslator chained = new DirectTranslator(new PrimitiveConverter());

		ListTranslator translator = new ListTranslator(chained);
		Set<Property> encoded = translator.encode(values, Path.EMPTY_PATH, true);

		// should be a single property with a list of values
		assertEquals(1, encoded.size());

		Field field = getClass().getDeclaredField("values");

		@SuppressWarnings("unchecked")
		List<String> decoded = (List<String>) translator.decode(encoded, Path.EMPTY_PATH, field.getGenericType());

		assertEquals("is", decoded.get(1));
		assertEquals(3, decoded.size());
	}

	@Test
	public void writeReadComplexList() throws SecurityException, NoSuchFieldException
	{
		// create dummy translator that always returns two properties
		PropertyTranslator chained = new PropertyTranslator()
		{
			public Set<Property> encode(Object object, Path path, boolean indexed)
			{
				HashSet<Property> properties = new HashSet<Property>();
				properties.add(new SimpleProperty(new Path.Builder(path).field("first").build(), object, indexed));
				properties.add(new SimpleProperty(new Path.Builder(path).field("second").build(), object, indexed));
				return properties;
			}

			public Object decode(Set<Property> properties, Path path, Type type)
			{
				return properties.iterator().next().getValue();
			}
		};

		ListTranslator translator = new ListTranslator(chained);
		Set<Property> encoded = translator.encode(values, Path.EMPTY_PATH, true);

		assertEquals(2, encoded.size());
		assertEquals(3, ((List<?>) encoded.iterator().next().getValue()).size());
		assertEquals(1, encoded.iterator().next().getPath().getParts().size());

		Field field = getClass().getDeclaredField("values");

		@SuppressWarnings("unchecked")
		List<String> decoded = (List<String>) translator.decode(encoded, Path.EMPTY_PATH, field.getGenericType());

		assertEquals("is", decoded.get(1));
		assertEquals(3, decoded.size());
	}

}
