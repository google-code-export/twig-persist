package com.google.code.twig;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Id;

public class ReferenceCycleTest extends LocalDatastoreTestCase
{
    static class First
    {
    	@Id int id;
        Second second;
    }

    static class Second
    {
        First first;
    }

    @Test
    public void referenceCycle()
    {
        ObjectDatastore datastore = new AnnotationObjectDatastore();
       
        First first = new First();
        Second second = new Second();
        first.second = second;
        first.id = 36;
        second.first = first;

        // store both 1st and 2nd
        Key firstKey = datastore.store(first);

        // forget everything
        datastore.disassociateAll();

        first = datastore.load(firstKey);
    }

}
