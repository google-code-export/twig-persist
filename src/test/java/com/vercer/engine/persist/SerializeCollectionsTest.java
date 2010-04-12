package com.vercer.engine.persist;


import java.util.Collection;

import junit.framework.Assert;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.vercer.engine.persist.annotation.AnnotationObjectDatastore;
import com.vercer.engine.persist.conversion.CombinedTypeConverter;
import com.vercer.engine.persist.conversion.DefaultTypeConverter;
import com.vercer.engine.persist.conversion.TypeConverter;


public class SerializeCollectionsTest extends LocalDatastoreTestCase
{
	@Test
	public void test()
	{
		ObjectDatastore datastore = new AnnotationObjectDatastore()
		{
			@Override
			protected TypeConverter createTypeConverter()
			{
				CombinedTypeConverter dtc = createDefaultTypeConverter();
				dtc.prepend(new DefaultTypeConverter.SlowBlobToAnything());
				dtc.prepend(new DefaultTypeConverter.SlowSerializableToBlob());
				return dtc;
			}
		};
		
		
		TypeWithCollection item = new TypeWithCollection();
		item.classes.add(String.class);
		item.classes.add(Integer.class);
		item.classes.add(Collection.class);
		
		Key key = datastore.store(item);
		
		datastore.disassociateAll();
		
		TypeWithCollection loaded = datastore.load(key);
		Assert.assertNotNull(loaded);
	}
}
