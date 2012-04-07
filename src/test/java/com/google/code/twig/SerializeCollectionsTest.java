package com.google.code.twig;


import java.util.Collection;

import junit.framework.Assert;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.TypeWithCollections.TypeWithEnum;
import com.google.code.twig.TypeWithCollections.TypeWithEnum.MyEnum;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.conversion.EngineConverters.BlobToObject;
import com.google.code.twig.conversion.EngineConverters.ObjectToBlob;
import com.vercer.convert.CombinedTypeConverter;


public class SerializeCollectionsTest extends LocalDatastoreTestCase
{
	@Test
	public void test()
	{
		ObjectDatastore datastore = new AnnotationObjectDatastore()
		{
			@Override
			public CombinedTypeConverter createStaticConverter()
			{
				CombinedTypeConverter combined = super.createStaticConverter();
				combined.register(new BlobToObject());
				combined.register(new ObjectToBlob());
				return combined;
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
