package com.google.code.twig;

import com.google.appengine.api.datastore.Query;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

import static org.junit.Assert.fail;

public class JoinTableTest extends LocalDatastoreTestCase
{
	public JoinTableTest()
	{
		ObjectDatastoreFactory.register(A.class);
		ObjectDatastoreFactory.register(B.class);
		ObjectDatastoreFactory.register(AB.class);
	}
	
	public int countTestCases()
	{
		return 0;
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
				ds.associatedKey(a)).now().next();

		if (!ab.b.equals(b))
			fail();
	}

}
