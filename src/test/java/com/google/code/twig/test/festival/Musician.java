package com.google.code.twig.test.festival;

import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.google.code.twig.annotation.Type;

public class Musician
{
	String name;
	Date birthday;
	
	@Type(Iterable.class)
	int[] favouriteNumbers;

	public Musician(String name)
	{
		this.name = name;
	}

	public Musician()
	{
	}

	@Override
	public String toString()
	{
		return "Musician [birthday=" + birthday + ", name=" + name + "]";
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((birthday == null) ? 0 : birthday.hashCode());
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
		if (!(obj instanceof Musician))
		{
			return false;
		}
		Musician other = (Musician) obj;
		if (birthday == null)
		{
			if (other.birthday != null)
			{
				return false;
			}
		}
		else if (!birthday.equals(other.birthday))
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
