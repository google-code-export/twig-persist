package com.google.code.twig;

import static org.junit.Assert.assertEquals;

import java.io.Serializable;
import java.util.HashMap;

import org.junit.Test;

import com.google.appengine.api.datastore.Blob;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Embedded;
import com.google.code.twig.annotation.Id;
import com.google.code.twig.annotation.Type;

public class HashMapTest extends LocalDatastoreTestCase
{
	public HashMapTest()
	{
		ObjectDatastoreFactory.register(InnerFoo.class);
		ObjectDatastoreFactory.register(Foo.class);
	}
	
	public static class InnerFoo implements Serializable
	{
		public InnerFoo(String name)
		{
			myName = name;
		}

		public InnerFoo()
		{
		}

		public String myName;
		private static final long serialVersionUID = 1L;
	}

	public static class Foo
	{
		@Id
		String myKey;
		@Embedded
		InnerFoo innerFoo;
		@Type(Blob.class)
		HashMap<String, InnerFoo> moreInnerFoos;
	}

	@Test
	public void embeddedQueryTest()
	{
		{
			ObjectDatastore datastore = new AnnotationObjectDatastore(false);

			Foo foo = new Foo();
			foo.myKey = "foo1";
			foo.innerFoo = new InnerFoo("foo1Name");
			foo.moreInnerFoos = new HashMap<String, InnerFoo>();
			foo.moreInnerFoos.put("hello", new InnerFoo("helloFoo"));
			foo.moreInnerFoos.put("goodbye", new InnerFoo("goodbyeFoo"));

			datastore.store(foo);
		}

		{
			ObjectDatastore datastore = new AnnotationObjectDatastore(false);
			Foo foundFoo = datastore.load(Foo.class, "foo1");

			assertEquals("foo1", foundFoo.myKey);
			assertEquals("foo1Name", foundFoo.innerFoo.myName);
			assertEquals(2, foundFoo.moreInnerFoos.size());
			assertEquals("helloFoo", foundFoo.moreInnerFoos.get("hello").myName);
			assertEquals("goodbyeFoo", foundFoo.moreInnerFoos.get("goodbye").myName);
		}
	}
}