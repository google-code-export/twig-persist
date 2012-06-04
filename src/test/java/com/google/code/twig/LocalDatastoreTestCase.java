package com.google.code.twig;

import org.junit.After;
import org.junit.Before;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;

public abstract class LocalDatastoreTestCase 
{
    private final LocalServiceTestHelper helper =
        new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()); //.setDefaultHighRepJobPolicyUnappliedJobPercentage(1));

    @Before
    public void setupDatastore() 
    {
        helper.setUp();
    }

    @After
    public void tearDownDatastore() 
    {
        helper.tearDown();
    }
}