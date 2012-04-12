package com.google.code.twig.test.festival;

import com.google.appengine.api.datastore.Text;
import com.google.code.twig.annotation.Type;

public class Track
{
	@Type(Text.class)
	String title;
	float length;
	
	SingleDetails details;

	@Override
	public String toString()
	{
		return "Track [details=" + details + ", length=" + length + ", title=" + title + "]";
	}
	
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