package com.vercer.engine.persist.festival;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.Locale;

import org.junit.Test;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.vercer.engine.persist.LocalDatastoreTestCase;
import com.vercer.engine.persist.TypesafeDatastore.FindOptions;
import com.vercer.engine.persist.annotation.AnnotationTypesafeDatastore;
import com.vercer.engine.persist.festival.Band.HairStyle;

public class MusicFestivalTestCase extends LocalDatastoreTestCase
{
	public Festival createFestival() throws ParseException
	{
		DateFormat dateFormat = new SimpleDateFormat("d MMM yyyy");

		Festival festival = new Festival();

		RockBand ledzep = new RockBand();
		ledzep.name = "Led Zeppelin";
		ledzep.locale = Locale.UK;
		ledzep.hair = Band.HairStyle.LONG_LIKE_A_GIRL;
		ledzep.chargedForBrokenTelevisions = true;

		Musician page = new Musician();
		page.name = "Jimmy Page";
		page.birthday = dateFormat.parse("9 January 1944");
		ledzep.members.add(page);

		Musician jones = new Musician();
		jones.name = "John Paul Jones";
		jones.birthday = dateFormat.parse("3 January 1946");
		ledzep.members.add(jones);

		Musician plant = new Musician();
		plant.name = "Robert Plant";
		plant.birthday = dateFormat.parse("20 August 1948");
		ledzep.members.add(plant);

		Musician bonham = new Musician();
		bonham.name = "John Bonham";
		bonham.birthday = dateFormat.parse("31 May 1948");
		ledzep.members.add(bonham);

		Album houses = new Album();
		houses.name = "Houses of the Holy";
		houses.released = dateFormat.parse("28 March 1973");
		houses.label = "Atlantic";
		houses.rocksTheHouse = true;
		houses.sold = 18000000;

		ledzep.albums.add(houses);

		houses.tracks = new Album.Track[3];
		houses.tracks[0] = new Album.Track();
		houses.tracks[0].title = "The Song Remains the Same";
		houses.tracks[0].length = 5.32f;
		houses.tracks[1] = new Album.Track();
		houses.tracks[1].title = "The Rain Song";
		houses.tracks[1].length = 7.39f;
		houses.tracks[2] = new Album.Track();
		houses.tracks[2].title = "Over the Hills and Far Away";
		houses.tracks[2].length = 4.50f;
//		houses.band = ledzep;

		Album iv = new Album();
		iv.name = "Led Zeppelin IV";
		iv.released = dateFormat.parse("8 November 1971");
		iv.label = "Atlantic";
		iv.rocksTheHouse = true;
		iv.sold = 22000000;
//		iv.band = ledzep;

		ledzep.albums.add(iv);

		festival.performances.add(ledzep);

		RockBand firm = new RockBand();
		firm.name = "The Firm";
		firm.hair = HairStyle.BALD;

		firm.members.add(page);

		Musician rogers = new Musician();
		rogers.name = "Paul Rogers";
		rogers.birthday = dateFormat.parse("17 December 1949");

		firm.members.add(rogers);

		festival.performances.add(firm);

		DanceBand soulwax = new DanceBand();
		soulwax.name = "Soulwax";
		soulwax.locale = new Locale("nl", "be");
		soulwax.members.add(new Musician("Stephen Dewaele"));
		soulwax.members.add(new Musician("David Dewaele"));
		soulwax.hair = Band.HairStyle.UNKEMPT_FLOPPY;
		soulwax.tabletsConfiscated = 12; // but they are still acting suspiciously

		Album swradio = new Album();
		swradio.name = "As Heard on Radio Soulwax Pt. 2";
		swradio.label = "Play It Again Sam";
		swradio.released = dateFormat.parse("17 February 2003");
		swradio.rocksTheHouse = true;
		swradio.sold = 500000;
//		swradio.band = soulwax;

		swradio.tracks = new Album.Track[2];
		swradio.tracks[0] = new Album.Track();
		swradio.tracks[0].title = "Where's Your Head At";
		swradio.tracks[0].length = 2.49f;
		swradio.tracks[1] = new Album.Track();
		swradio.tracks[1].title = "A really long track name that is certainly over 500 chars" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again" +
				"long expecially because it is repeated again and again and again";

		swradio.tracks[1].length = 1.38f;

		festival.performances.add(soulwax);

		return festival;
	}

	@Test
	public void rock() throws ParseException
	{
		Festival festival = createFestival();
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		AnnotationTypesafeDatastore typesafe = new AnnotationTypesafeDatastore(service);

		Key key = typesafe.store(festival);

		AnnotationTypesafeDatastore typesafe2 = new AnnotationTypesafeDatastore(service);

		Object reloaded = typesafe2.load(key);

		// they should be different instances from distinct sessions
		assertNotSame(reloaded, festival);
		
		// they should have the same data
		assertEquals(reloaded, festival);
	}
	
	@Test
	public void hair() throws ParseException
	{
		Festival festival = createFestival();
		DatastoreService service = DatastoreServiceFactory.getDatastoreService();
		AnnotationTypesafeDatastore typesafe = new AnnotationTypesafeDatastore(service);
		typesafe.store(festival);
		
		FindOptions options = new FindOptions();
		options.setEntityPredicate(new Predicate<Entity>()
		{
			public boolean apply(Entity input)
			{
				return input.getKey().getName().equals("Led Zeppelin");
			}
		});
		Iterator<RockBand> results = typesafe.find(RockBand.class, options);
		assertEquals(Iterators.size(results), 1);
	}
	
}
