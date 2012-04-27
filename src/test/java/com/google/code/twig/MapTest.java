package com.google.code.twig;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import junit.framework.Assert;

import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class MapTest extends LocalDatastoreTestCase
{
	public static class ContainsMaps
	{
		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((blank == null) ? 0 : blank.hashCode());
			result = prime * result + ((initialised == null) ? 0 : initialised.hashCode());
			result = prime * result + ((name == null) ? 0 : name.hashCode());
			result = prime * result + ((nulled == null) ? 0 : nulled.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ContainsMaps other = (ContainsMaps) obj;
			if (blank == null)
			{
				if (other.blank != null)
					return false;
			}
			else if (!blank.equals(other.blank))
				return false;
			if (initialised == null)
			{
				if (other.initialised != null)
					return false;
			}
			else if (!initialised.equals(other.initialised))
				return false;
			if (name == null)
			{
				if (other.name != null)
					return false;
			}
			else if (!name.equals(other.name))
				return false;
			if (nulled == null)
			{
				if (other.nulled != null)
					return false;
			}
			else if (!nulled.equals(other.nulled))
				return false;
			return true;
		}
		@Override
		public String toString()
		{
			return "ContainsMaps [blank=" + blank + ", initialised=" + initialised + ", name="
					+ name + ", nulled=" + nulled + "]";
		}

		String name;
		Map<String, Contained> blank;
		Map<?, ?> nulled;
		Map<Long, Contained> initialised = new TreeMap<Long, Contained>();
		Map<String, Set<String>> stingToStringSet;
	}
	
	public static class Contained implements Comparable<Contained>
	{
		@Override
		public String toString()
		{
			return "Contained [message=" + message + ", number=" + number + "]";
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((message == null) ? 0 : message.hashCode());
			result = prime * result + number;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Contained other = (Contained) obj;
			if (message == null)
			{
				if (other.message != null)
					return false;
			}
			else if (!message.equals(other.message))
				return false;
			if (number != other.number)
				return false;
			return true;
		}

		String message;
		int number;
		
		Contained()
		{
		}
		
		public Contained(String message, int number)
		{
			this.message = message;
			this.number = number;
		}

		@Override
		public int compareTo(Contained o)
		{
			return message.compareTo(o.message);
		}
	}
	
	@Test
	public void storeLoad()
	{
		ContainsMaps created = new ContainsMaps();
		created.name = "first";
		created.blank = new HashMap<String, Contained>();
		created.blank.put("c1", new Contained("im the first", 99));
		created.blank.put("c2", new Contained("im the second", 84));
		created.initialised.put(45l, new Contained("dated one", 32));
		created.initialised.put(67l, new Contained("dated today", 18));
		created.stingToStringSet = new HashMap<String, Set<String>>();
		
		Set<String> values = new java.util.HashSet<String>();
		values.add("There");
		created.stingToStringSet.put("Hi", values);
		
		AnnotationObjectDatastore datastore = new AnnotationObjectDatastore();
		Key key = datastore.store(created);
		datastore.disassociateAll();
		Object loaded = datastore.load(key);
		Assert.assertEquals(created, loaded);
	}
}
