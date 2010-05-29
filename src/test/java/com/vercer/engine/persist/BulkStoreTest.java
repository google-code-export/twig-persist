package com.vercer.engine.persist;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.vercer.engine.persist.annotation.AnnotationObjectDatastore;
import com.vercer.engine.persist.annotation.Key;
import com.vercer.engine.persist.annotation.Store;

public class BulkStoreTest extends LocalDatastoreTestCase {
        public static class ExampleModel {
                @Key
                private Long autoId;
                private String field;
                @Store(true)
                private transient String transientField;

                public void setAutoId(Long autoId) {
                        this.autoId = autoId;
                }

                public long getAutoId() {
                        return autoId;
                }

                public void setField(String field) {
                        this.field = field;
                }

                public String getField() {
                        return field;
                }

                public void setTransientField(String transientField) {
                        this.transientField = transientField;
                }

                public String getTransientField() {
                        return transientField;
                }
        }

        @Test
        public void bulkStoreAndRefreshOnSimpleModelShouldAssignKeysForEntities() {
                ObjectDatastore ods = new AnnotationObjectDatastore();
                ExampleModel[] models = new ExampleModel[] { new ExampleModel(),
                                new ExampleModel() };
                ods.storeAll(Lists.newArrayList(models));
                for (ExampleModel model : models) {
                        ods.refresh(model);
                        assertTrue(model.getAutoId() != 0);
                }
        }
}