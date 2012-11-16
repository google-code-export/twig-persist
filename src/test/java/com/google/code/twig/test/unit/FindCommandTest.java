package com.google.code.twig.test.unit;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.ObjectDatastoreFactory;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.test.space.Pilot;
import com.google.code.twig.test.space.RocketShip;
import com.google.code.twig.test.space.RocketShip.Planet;
import com.google.common.collect.Lists;

public class FindCommandTest extends LocalDatastoreTestCase
{
	private ObjectDatastore datastore;

	public FindCommandTest()
	{
		ObjectDatastoreFactory.register(RocketShip.class);
		ObjectDatastoreFactory.register(Pilot.class);
	}
	
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
			.addFilter("destination", FilterOperator.EQUAL, Planet.MARS)
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
