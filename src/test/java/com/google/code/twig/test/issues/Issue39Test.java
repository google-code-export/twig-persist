package com.google.code.twig.test.issues;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.google.code.twig.LocalDatastoreTestCase;
import com.google.code.twig.ObjectDatastore;
import com.google.code.twig.annotation.AnnotationObjectDatastore;

public class Issue39Test extends LocalDatastoreTestCase
{
	public static class Restaurant implements Serializable
	{
		private static final long serialVersionUID = 1L;
		String restaurantName;
	}

	public static class City implements Serializable
	{
		private static final long serialVersionUID = 1L;
		String cityName;
		List<Restaurant> restaurants = new ArrayList<Issue39Test.Restaurant>();

		public City(String name)
		{
			super();
			this.cityName = name;
		}

		public List<Restaurant> getRestaurants()
		{
			return restaurants;
		}
	}

	@Test
	public void createRestaurant()
	{
		ObjectDatastore od = new AnnotationObjectDatastore();
		
		City city = new City("Dupe");
		od.store(city);
		
		Restaurant restaurant = new Restaurant();
		city.getRestaurants().add(restaurant);
		od.update(city);
		
		Iterator<City> cities = od.find(City.class);
		Assert.assertNotNull(cities.next());
		Assert.assertFalse(cities.hasNext());
	}
}