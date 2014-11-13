
/**
 * astro: astronomical functions, utilities and data
 * 
 * This package was created by Ian Cameron Smith in February 2009, based
 * on the formulae in "Practical Astronomy with your Calculator" by
 * Peter Duffett-Smith, ISBN-10: 0521356997.
 * 
 * Note that the formulae have been converted to work in radians, to
 * make it easier to work with java.lang.Math.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.test.astro;


import static org.hermit.test.NumericAsserts.assertTolerance;
import junit.framework.TestCase;

import org.hermit.astro.AstroError;
import org.hermit.astro.Body;
import org.hermit.astro.Observation;
import org.hermit.geo.Position;


/**
 * Test code.
 */
public class TestRiseSet
	extends TestCase
{
 
	// ******************************************************************** //
	// Rise and Set.
	// ******************************************************************** //

	private static void testRiseSet(String lab, Body b,
									double cr, double ct, double cs)
		throws AstroError
	{
		double r = b.get(Body.Field.RISE_TIME);
		double t = b.get(Body.Field.TRANSIT_TIME);
		double s = b.get(Body.Field.SET_TIME);
		
		assertTolerance(lab + " rise", r, cr, 0.0004);
		assertTolerance(lab + " tran", t, ct, 0.0004);
		assertTolerance(lab + " set", s, cs, 0.0004);
	}


	private static void testTwilight(String lab, Body b, double cr, double cs)
		throws AstroError
	{
		double r = b.get(Body.Field.RISE_TWILIGHT);
		double s = b.get(Body.Field.SET_TWILIGHT);
		
		assertTolerance(lab + " rise t", r, cr, 0.02);
		assertTolerance(lab + " set t", s, cs, 0.02);
	}


	public void testVenusRiseSet() throws AstroError {
		Observation o = new Observation();
		o.setDate(1988, 3, 20.0);
		o.setObserverPosition(Position.fromDegrees(42.3333, -71.0833));
		Body venus = o.getBody(Body.Name.VENUS);
		testRiseSet("Meeus 15", venus, 24 * 0.51766, 24 * 0.81980, 24 * 0.12130);
	}


	public void testSunRiseSet() throws AstroError {
		Observation o = new Observation();
		o.setDate(1979, 9, 7.0);
		o.setObserverPosition(Position.fromDegrees(52, 0));
		Body sun = o.getBody(Body.Name.SUN);
		
		testRiseSet("PAC 50", sun, 5.338, 11.9696, 18.583);
	}


	public void testTwilight() throws AstroError {
		Observation o = new Observation();
		o.setDate(1979, 9, 7.0);
		o.setObserverPosition(Position.fromDegrees(52, 0));
		Body sun = o.getBody(Body.Name.SUN);
		
		testTwilight("PAC 50", sun, 3.283, 20.616667);
	}

}

