package com.google.code.twig.test.space;

import com.google.code.twig.annotation.Denormalise;
import com.google.code.twig.annotation.Id;

public class Pilot
{
	@Id 
	private long id;
	private String name;
	
	@Denormalise("destination")
	private RocketShip spaceship;
	
	protected Pilot()
	{
	}
	
	public Pilot(String name, RocketShip spaceship)
	{
		this.name = name;
		this.spaceship = spaceship;
	}
	
	public long getId()
	{
		return this.id;
	}
	
	public String getName()
	{
		return this.name;
	}
	
	public RocketShip getSpaceship()
	{
		return this.spaceship;
	}
}