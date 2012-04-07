package com.google.code.twig.test.space;

import java.util.HashSet;
import java.util.Set;

import com.google.code.twig.annotation.Denormalise;
import com.google.code.twig.annotation.Id;

public class Mission
{
	@Id private String goal;

	@Denormalise({"name", "spaceship", "spaceship.destination"})
	private Set<Pilot> pilots = new HashSet<Pilot>();
	
	private SpaceStation station;
	
	protected Mission()
	{
	}
	
	public Mission(String name)
	{
		this.goal = name;
	}
	
	public void setStation(SpaceStation station)
	{
		this.station = station;
	}
	
	public SpaceStation getStation()
	{
		return this.station;
	}
	
	public Set<Pilot> getPilots()
	{
		return this.pilots;
	}
	
	public String getGoal()
	{
		return this.goal;
	}
}
