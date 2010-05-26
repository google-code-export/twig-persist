package com.vercer.engine.persist;

import com.google.appengine.api.datastore.Query;
import com.vercer.engine.persist.ObjectDatastore;
import com.vercer.engine.persist.annotation.AnnotationObjectDatastore;
import junit.framework.Test;
import junit.framework.TestResult;

import static org.junit.Assert.fail;

public class JoinTableTest extends LocalDatastoreTestCase
{

	public int countTestCases()
	{
		return 0; // To change body of implemented methods use File | Settings |
					// File Templates.
	}

	public static class A
	{
	}

	public static class B
	{
	}

	public static class AB
	{
		A a;
		B b;

		public AB()
		{
		}

		public AB(A a, B b)
		{
			// To change body of created methods use File | Settings | File
			// Templates.
			this.a = a;
			this.b = b;
		}
	}

	@org.junit.Test
	public void testJoinTable()
	{
		ObjectDatastore ds = new AnnotationObjectDatastore();

		A a;
		B b;
		ds.store(a = new A());
		ds.store(b = new B());
		ds.store(new AB(a, b));

		final AB ab = ds.find().type(AB.class).addFilter("a", Query.FilterOperator.EQUAL,
				ds.associatedKey(a)).returnResultsNow().next();

		if (!ab.b.equals(b))
			fail();
	}

}
