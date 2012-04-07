package com.google.code.twig.standard;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.PropertyTranslator;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.test.space.Mission;
import com.google.code.twig.test.space.Pilot;
import com.google.code.twig.test.space.RocketShip;
import com.google.code.twig.test.space.SpaceStation;
import com.google.code.twig.test.space.RocketShip.Planet;

public class TranslatorObjectDatastoreTest extends LocalDatastoreTestCase
{
	private AnnotationObjectDatastore datastore;

	@Before
	public void createDatastore()
	{
		this.datastore = new AnnotationObjectDatastore();
	}

	@Test
	public void associateObjectWithSameKey()
	{
		// create and store a station
		SpaceStation station = new SpaceStation("behemoth");
		datastore.store(station);
		
		// associating a new station with the same key will return the same instance
		SpaceStation associated = datastore.associate(new SpaceStation("behemoth"));
		Assert.assertSame(station, associated);
	}
	
	@Test
	public void associateGraphWithSameKey()
	{
		// create and store a station
		SpaceStation station = new SpaceStation("behemoth");
		datastore.store(station);
		
		// associating an instance that references an unassociated station
		// should throw an exception.
		Mission exploration = new Mission("Exploration");
		exploration.setStation(new SpaceStation("behemoth"));
		
		boolean threw = false;
		try
		{
			datastore.associate(exploration);
		}
		catch (Exception e)
		{
			threw = true;
		}
		Assert.assertTrue(threw);

		// associating an instance that references an associated station
		Mission domination = new Mission("Domination");
		SpaceStation dominstation = new SpaceStation("behemoth");
		
		Assert.assertNotSame(domination, station);
		dominstation = datastore.associate(dominstation);
		Assert.assertSame(dominstation, station);
		
		domination.setStation(dominstation);

		// now associate a new mission which references existing station
		Mission associated = datastore.associate(domination);
		
		// the same instance should be returned when not already associated
		Assert.assertSame(associated, domination);
		
		// just check that the station is still the same one
		Assert.assertSame(station, associated.getStation());
	}
	
	@Test
	public void denormalise() throws EntityNotFoundException
	{
		Mission mission = new Mission("conquor");
		mission.getPilots().add(new Pilot("bob", new RocketShip(Planet.MARS)));
		
		Key key = datastore.store(mission);

		// check we have only the right amount of properties stored
		Entity entity = datastore.getDefaultService().get(key);
		Assert.assertEquals(entity.getProperties().size(), 5);
		
		datastore.disassociateAll();
		
		Mission loaded = datastore.load().key(key).activate(0).now();
		
		Assert.assertFalse(datastore.isActivatable(mission));
		
		Pilot pilot = loaded.getPilots().iterator().next();
		
		Assert.assertFalse(datastore.isActivated(pilot));
		
		// the pilot is unactivated but its name was set
		Assert.assertEquals(pilot.getName(), "bob");
		
		Assert.assertEquals(pilot.getSpaceship().getDestination(), RocketShip.Planet.MARS);
	}
}

