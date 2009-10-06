package com.vercer.engine.persist.festival;

import java.util.Date;

public class Musician extends ReflectiveObject
{
	public Musician(String name)
	{
		super(true);
		this.name = name;
	}

	public Musician()
	{
	}

	String name;
	Date birthday;
}
