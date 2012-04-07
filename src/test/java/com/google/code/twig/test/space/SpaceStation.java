package com.google.code.twig.test.space;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.google.code.twig.annotation.Id;

public class SpaceStation
{
	@Id private String name;
	private Collection<RocketShip> rockets = new ArrayList<RocketShip>();
	
	protected SpaceStation()
	{		
	}
	
	public SpaceStation(String name)
	{
		this.name = name;
	}

	public void addRocketShip(RocketShip rocket)
	{
		this.rockets.add(rocket);
	}
	
	public Collection<RocketShip> getRockets()
	{
		return Collections.unmodifiableCollection(this.rockets);
	}
	
	public String getName()
	{
		return this.name;
	}
}
