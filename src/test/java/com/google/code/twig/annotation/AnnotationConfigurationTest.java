package com.google.code.twig.annotation;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import org.junit.Test;
import org.junit.Before;

public class AnnotationConfigurationTest {

	public String naked;
	
	@GaeKey
	public String gaeKey;
	
	protected AnnotationConfiguration config;
	
	@Before
	public void setUp() {
		config = new AnnotationConfiguration(true, 1);
	}
	
	@Test
	public void testGaeKeyPresent() throws NoSuchFieldException {
		
		boolean present = config.gaeKey(getClass().getField("gaeKey"));
		assertTrue(present);
	}
	
	@Test
	public void testGaeKeyNotPresent() throws NoSuchFieldException {
		
		boolean present = config.gaeKey(getClass().getField("naked"));
		assertFalse(present);
	}
}
