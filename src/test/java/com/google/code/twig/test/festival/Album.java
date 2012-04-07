/**
 *
 */
package com.google.code.twig.test.festival;

import java.util.Arrays;
import java.util.Date;

import com.google.appengine.api.datastore.Text;
import com.google.code.twig.annotation.Embedded;
import com.google.code.twig.annotation.Id;
import com.google.code.twig.annotation.Type;

public class Album
{
	@Id
	String name;
	String label;

//	@Entity(Relationship.PARENT)
//	Band band;

	Date released;
	boolean rocksTheHouse;
	long sold;

	@Embedded
	Track[] tracks;

	public static class Track
	{
		@Override
		public String toString()
		{
			return "Track [details=" + details + ", length=" + length + ", title=" + title + "]";
		}

		public static class SingleDetails
		{
			String bside;
			int released;
		}
		
		@Type(Text.class)
		String title;
		float length;
		
		SingleDetails details;

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + Float.floatToIntBits(length);
			result = prime * result + ((title == null) ? 0 : title.hashCode());
			return result;
		}
		
		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
			{
				return true;
			}
			if (obj == null)
			{
				return false;
			}
			if (!(obj instanceof Track))
			{
				return false;
			}
			Track other = (Track) obj;
			if (Float.floatToIntBits(length) != Float.floatToIntBits(other.length))
			{
				return false;
			}
			if (title == null)
			{
				if (other.title != null)
				{
					return false;
				}
			}
			else if (!title.equals(other.title))
			{
				return false;
			}
			return true;
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
			System.out.println("Tracks not equal: " + this + " : " + other);
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