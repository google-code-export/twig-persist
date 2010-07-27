package com.google.code.twig;


import java.util.Collection;

import junit.framework.Assert;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.TypeWithCollections.TypeWithEnum;
import com.google.code.twig.TypeWithCollections.TypeWithEnum.MyEnum;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.conversion.CombinedTypeConverter;
import com.google.code.twig.conversion.DefaultTypeConverter;


public class SerializeCollectionsTest extends LocalDatastoreTestCase
{
	@Test
	public void test()
	{
		ObjectDatastore datastore = new AnnotationObjectDatastore()
		{
			@Override
			protected CombinedTypeConverter createTypeConverter()
			{
				DefaultTypeConverter dtc = new DefaultTypeConverter();
				dtc.prepend(new DefaultTypeConverter.SlowBlobToAnything());
				dtc.prepend(new DefaultTypeConverter.SlowSerializableToBlob());
				return dtc;
			}
		};
		
		
		TypeWithCollections item = new TypeWithCollections();
		item.classes.add(String.class);
		item.classes.add(Integer.class);
		item.classes.add(Collection.class);
		
		Key key = datastore.store(item);
		
		TypeWithEnum thing = new TypeWithEnum();
		thing.watsit = MyEnum.TWOK10;
		item.things.add(thing);
		item.things.add(null);
		
		datastore.disassociateAll();
		
		TypeWithCollections loaded = datastore.load(key);
		Assert.assertNotNull(loaded);
	}
}
