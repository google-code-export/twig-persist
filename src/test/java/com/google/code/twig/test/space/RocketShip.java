package com.google.code.twig.test.space;

import java.util.ArrayList;
import java.util.List;



public class RocketShip
{
	public enum Planet { MARS, VENUS, MERCURY };
	
	private Planet destination;
	
	private List<Pilot> pilots = new ArrayList<Pilot>();
	
	protected RocketShip()
	{
	}
	
	public RocketShip(Planet planet)
	{
		this.destination = planet;
	}
	
	public Planet getDestination()
	{
		return this.destination;
	}
	
	public List<Pilot> getPilots()
	{
		return this.pilots;
	}
}