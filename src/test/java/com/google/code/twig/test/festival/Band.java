/**
 *
 */
package com.google.code.twig.test.festival;

import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import com.google.code.twig.annotation.Child;
import com.google.code.twig.annotation.Id;
import com.google.code.twig.annotation.Type;

public class Band
{
	enum HairStyle { LONG_LIKE_A_GIRL, UNKEMPT_FLOPPY, NAVY_SHORT, BALD };

	@Id
	String name;
	
	Locale locale;

	LinkedList<Musician> members = new LinkedList<Musician>();

	@Child List<Album> albums;
	Band.HairStyle hair;
	
	@Override
	public String toString()
	{
		return "Band [albums=" + albums + ", hair=" + hair + ", locale=" + locale + ", members="
				+ members + ", name=" + name + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((albums == null) ? 0 : albums.hashCode());
		result = prime * result + ((hair == null) ? 0 : hair.hashCode());
		result = prime * result + ((locale == null) ? 0 : locale.hashCode());
		result = prime * result + ((members == null) ? 0 : members.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		if (!(obj instanceof Band))
		{
			return false;
		}
		Band other = (Band) obj;
		if (albums == null)
		{
			if (other.albums != null)
			{
				return false;
			}
		}
		else if (!albums.equals(other.albums))
		{
			return false;
		}
		if (hair == null)
		{
			if (other.hair != null)
			{
				return false;
			}
		}
		else if (!hair.equals(other.hair))
		{
			return false;
		}
		if (locale == null)
		{
			if (other.locale != null)
			{
				return false;
			}
		}
		else if (!locale.equals(other.locale))
		{
			return false;
		}
		if (members == null)
		{
			if (other.members != null)
			{
				return false;
			}
		}
		else if (!members.equals(other.members))
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
		return true;
	}

}