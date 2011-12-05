package com.google.code.twig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Id;
import com.google.code.twig.annotation.Store;

public class StoreTest extends LocalDatastoreTestCase
{
	public static class ExampleModel
	{
		@Id
		private Long autoId;
		private String field;
		@Store(true)
		private transient String transientField;

		public void setAutoId(Long autoId)
		{
			this.autoId = autoId;
		}

		public long getAutoId()
		{
			return autoId;
		}

		public void setField(String field)
		{
			this.field = field;
		}

		public String getField()
		{
			return field;
		}

		public void setTransientField(String transientField)
		{
			this.transientField = transientField;
		}

		public String getTransientField()
		{
			return transientField;
		}
	}

	@Test
	public void bulkStoreAndRefreshOnSimpleModelShouldAssignKeysForEntities()
	{
		ObjectDatastore ods = new AnnotationObjectDatastore();
		ExampleModel[] models = new ExampleModel[] { new ExampleModel(), new ExampleModel() };
		ods.storeAll(Lists.newArrayList(models));
		for (ExampleModel model : models)
		{
			ods.refresh(model);
			assertTrue(model.getAutoId() != 0);
		}
	}

	@Test
	public void transientFieldWithStoreEqualsTrueShouldBePersisted()
	{
		ObjectDatastore ods1 = new AnnotationObjectDatastore();
		ExampleModel model = new ExampleModel();
		model.setTransientField("xxx");
		ods1.store(model);
		ObjectDatastore ods2 = new AnnotationObjectDatastore();
		ExampleModel reloadedModel = ods2.load(ods1.associatedKey(model));
		assertEquals("xxx", reloadedModel.getTransientField());
	}
	
}