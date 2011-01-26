package com.google.code.twig;

import java.util.Date;

import junit.framework.Assert;

import org.junit.Test;

import com.google.appengine.api.datastore.QueryResultIterator;
import com.google.code.twig.annotation.AnnotationObjectDatastore;
import com.google.code.twig.annotation.Embed;
import com.google.code.twig.annotation.Key;

@SuppressWarnings("deprecation")
class Driver {
	@Key public Long id;
	@Embed(polymorphic=true) public Vehicle vehicle;
	public Driver() {}
	public Driver(Vehicle vehicle) {this.vehicle = vehicle;}
	
	public Long getName() {return id;}
	public Vehicle getVehicle() {return vehicle;}
	@Override
	public String toString() {
		return "Test [" + (id != null ? "id=" + id + ", " : "")
				+ (vehicle != null ? "vehicle=" + vehicle + ", " : "")
				+ "]";}
}

class Vehicle {
	public String type;
	public Vehicle() {}
	public Vehicle(String type) {this.type = type;}
	public String getType() {return type;}
	@Override
	public String toString() {return "Vehicle [type=" + type + "]";}
}

class Car extends Vehicle {
	public Date rentDate;
	public Car() {}
	public Car(Date rentDate){super("ground");this.rentDate = rentDate;}
	public Date getRentDate() {return rentDate;}
	@Override
	public String toString() {return "vehicle [rentDate=" + rentDate + "]";}
}

class Lorry extends Vehicle {
	public Long maxSpeed;
	public Lorry() {}
	public Lorry(Long maxSpeed) {super("ground");this.maxSpeed = maxSpeed;}
	public Long getTestTriple() {return maxSpeed;}
	@Override
	public String toString() {return "lorry [Maxspeed=" + maxSpeed + "]";}
}

public class PolymorphicCollectionsTest extends LocalDatastoreTestCase
{
	@Test
	public void test()
	{
		ObjectDatastore datastore = new AnnotationObjectDatastore();
			
		for(int i=0;i<10; i++){
			datastore.store(new Driver(new Car(new Date())));
			datastore.store(new Driver(new Lorry(new Long(50+i))));
		}
		
		QueryResultIterator<Driver> result = datastore.find().type(Driver.class).unactivated().now();
		
		Assert.assertTrue(result.hasNext());
	}
}
