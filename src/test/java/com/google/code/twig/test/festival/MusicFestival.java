/**
 *
 */
package com.google.code.twig.test.festival;

import java.util.ArrayList;
import java.util.List;


public class MusicFestival
{
	public List<Band> bands = new ArrayList<Band>();

	@Override
	public String toString()
	{
		return "Festival [performances=" + bands + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((bands == null) ? 0 : bands.hashCode());
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
		if (!(obj instanceof MusicFestival))
		{
			return false;
		}
		MusicFestival other = (MusicFestival) obj;
		if (bands == null)
		{
			if (other.bands != null)
			{
				return false;
			}
		}
		else if (!bands.equals(other.bands))
		{
			return false;
		}
		return true;
	}
}