package com.google.code.twig.test.unit;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.repackaged.com.google.common.collect.Lists;
import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.test.space.Pilot;
import com.google.code.twig.test.space.RocketShip;
import com.google.code.twig.test.space.RocketShip.Planet;

public class FindCommandTest extends LocalDatastoreTestCase
{
	private ObjectDatastore datastore;

	@Before
	public void setup()
	{
		datastore = new AnnotationObjectDatastore();
	}
	
	@Test
	public void filterWithEnumValue()
	{
		datastore.store(new RocketShip(Planet.MARS));
		datastore.store(new RocketShip(Planet.VENUS));
		datastore.store(new RocketShip(Planet.MERCURY));

		RocketShip result = datastore.find()
			.type(RocketShip.class)
			.addFilter("planet", FilterOperator.EQUAL, Planet.MARS)
			.returnUnique()
			.now();
		
		assertNotNull(result);
	}
	
	@Test
	public void filterOnRelatedInstanceDirectly()
	{
		RocketShip urisSpaceship = new RocketShip(Planet.MARS);
		Pilot uriGaragrin = new Pilot("Uri Gagagrin", urisSpaceship);
		datastore.store(uriGaragrin);
		
		Pilot shouldBeUri = datastore.find()
			.type(Pilot.class)
			.addFilter("spaceship", FilterOperator.EQUAL, urisSpaceship)
			.returnUnique()
			.now();
		
		assertSame(uriGaragrin, shouldBeUri);
	}

	@Test
	public void filterOnRelatedInstanceByKey()
	{
		RocketShip laikasSpaceship = new RocketShip(Planet.VENUS);
		Pilot laikaSpaceDog = new Pilot("Laika", laikasSpaceship);
		datastore.store(laikaSpaceDog);
		
		Key laikasShipKey = datastore.associatedKey(laikasSpaceship);
		
		Pilot shouldBeLaika = datastore.find()
			.type(Pilot.class)
			.addFilter("spaceship", FilterOperator.EQUAL, laikasShipKey)
			.returnUnique()
			.now();
		
		assertSame(laikaSpaceDog, shouldBeLaika);
	}
	
	@Test
	public void findInstanceById()
	{
		Pilot chopSpaceChimp = new Pilot("Chop Chop Chang", null);
		datastore.store(chopSpaceChimp);
		
		Pilot shouldBeChopChop = datastore.find()
			.type(Pilot.class)
			.addFilter("id", FilterOperator.EQUAL, chopSpaceChimp.getId())
			.returnUnique()
			.now();
		
		assertSame(chopSpaceChimp, shouldBeChopChop);
	}
	
	@Test
	public void filterOnRelatedInstanceDirectlyUsingIN()
	{
		RocketShip neilsSpaceship = new RocketShip(Planet.MARS);
		Pilot neilArmstrong = new Pilot("Neil Armstrong", neilsSpaceship);
		datastore.store(neilArmstrong);
		
		RocketShip redDwarf = new RocketShip(Planet.MERCURY);
		datastore.store(redDwarf);
		
		Collection<RocketShip> ships = Lists.newArrayList(redDwarf, neilsSpaceship);
		
		Pilot shouldBeUri = datastore.find()
			.type(Pilot.class)
			.addFilter("spaceship", FilterOperator.IN, ships)
			.returnUnique()
			.now();
		
		assertSame(neilArmstrong, shouldBeUri);
	}
}
