/**
 *
 */
package com.vercer.engine.persist.festival;

import java.util.Arrays;
import java.util.Date;

import com.google.appengine.api.datastore.Blob;
import com.vercer.engine.persist.ReflectiveObject;
import com.vercer.engine.persist.annotation.Embed;
import com.vercer.engine.persist.annotation.Entity;
import com.vercer.engine.persist.annotation.Key;
import com.vercer.engine.persist.annotation.Type;
import com.vercer.engine.persist.annotation.Entity.Relationship;

public class Album
{
	@Key
	String name;
	String label;

//	@Entity(Relationship.PARENT)
//	Band band;

	Date released;
	boolean rocksTheHouse;
	long sold;

	@Embed
	Track[] tracks;

	public static class Track extends ReflectiveObject
	{
		String title;
		float length;

		public Track()
		{
			super(true);
		}
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
//		result = prime * result + ((band == null) ? 0 : band.hashCode());
		result = prime * result + ((label == null) ? 0 : label.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((released == null) ? 0 : released.hashCode());
		result = prime * result + (rocksTheHouse ? 1231 : 1237);
		result = prime * result + (int) (sold ^ (sold >>> 32));
		result = prime * result + Arrays.hashCode(tracks);
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (!(obj instanceof Album))
		{
			return false;
		}
		Album other = (Album) obj;
		if (label == null)
		{
			if (other.label != null)
			{
				return false;
			}
		}
		else if (!label.equals(other.label))
		{
			return false;
		}
		if (name == null)
		{
			if (other.name != null)
			{
				return false;
			}
		}
		else if (!name.equals(other.name))
		{
			return false;
		}
		if (released == null)
		{
			if (other.released != null)
			{
				return false;
			}
		}
		else if (!released.equals(other.released))
		{
			return false;
		}
		if (rocksTheHouse != other.rocksTheHouse)
		{
			return false;
		}
		if (sold != other.sold)
		{
			return false;
		}
		if (!Arrays.equals(tracks, other.tracks))
		{
			return false;
		}
		return true;
	}

	@Override
	public String toString()
	{
		return "Album [" /* band=" +  band + ", */ + "label=" + label + ", name=" + name + ", released="
				+ released + ", rocksTheHouse=" + rocksTheHouse + ", sold=" + sold + ", tracks="
				+ Arrays.toString(tracks) + "]";
	}
}