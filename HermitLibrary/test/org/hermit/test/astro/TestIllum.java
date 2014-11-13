
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


import static java.lang.Math.toDegrees;
import static org.hermit.test.NumericAsserts.assertDegrees;
import static org.hermit.test.NumericAsserts.assertTolerance;
import junit.framework.TestCase;

import org.hermit.astro.AstroError;
import org.hermit.astro.Body;
import org.hermit.astro.Instant;
import org.hermit.astro.Moon;
import org.hermit.astro.Observation;
import org.hermit.astro.Planet;
import org.hermit.astro.Sun;
import org.hermit.astro.Body.Field;
import org.hermit.geo.Position;


/**
 * Test code.
 */
public class TestIllum
	extends TestCase
{
 
	// ******************************************************************** //
	// Phase.
	// ******************************************************************** //

	private static void testPhase(Body b, double cp, double cl) throws AstroError {
		String n = b.getId().toString();
		String lab = "SkyPosition " + n;

		double k = b.get(Field.PHASE);
		double Χ = Math.toDegrees(b.get(Field.ABS_BRIGHT_LIMB));
		
		assertTolerance(lab + " phase", k, cp, 0.002);
		assertDegrees(lab + " limb", Χ, cl, 0.2);
	}


	public void testPhase() throws AstroError {
		// From Meeus chapter 41.  Bright limb added manually.
		Instant instant = Instant.fromTd(1992, 12, 20.0);
		Observation o = new Observation(instant);
		Planet venus = o.getPlanet(Planet.Name.VENUS);
		testPhase(venus, 0.647, 255.42);
		
		// From Meeus chapter 48.
		instant = Instant.fromTd(1992, 4, 12.0);
		o.setTime(instant);
		Moon moon = o.getMoon();
		testPhase(moon, 0.6786, 285.0);
	}
	
	 
	// ******************************************************************** //
	// Distance and Size.
	// ******************************************************************** //

	private static void testDistSize(String lab, Body b, double cr, double cd)
		throws AstroError
	{
		double r = b.get(Field.EARTH_DISTANCE);
		double d = toDegrees(b.get(Field.APPARENT_DIAMETER)) * 3600;
		
		assertTolerance(lab + " dist", r, cr, 0.0002);
		assertTolerance(lab + " size", d, cd, 0.34);
	}
	

	public void testSunDistSize() throws AstroError {
		// From NASA Horizons:
		//	Sun 1988-Jul-27 16:30 UT
		//	Dist = 1.01539413765642 AU
		//	Diam = 1890.192 s
		Observation o = new Observation();
		o.setTime(new Instant(1988, 7, 27, 16, 30, 0));
		o.setObserverPosition(Position.fromDegrees(42.3333, -71.0833));
		Sun s = o.getSun();
		testDistSize("NH 1988-Jul-27", s, 1.01539413765642, 1890.192);
	}
	

	public void testMoonDistSize() throws AstroError {
		// From NASA Horizons:
		//	Moon 1988-Jul-27 00:30 UT
		//	Altitude = 11.7334°
		//	Dist 	 = 0.002454383735427 AU
		//	Diam 	 = 1952.040 s
		Observation o = new Observation();
		o.setTime(new Instant(1988, 7, 27, 0, 30, 0));
		o.setObserverPosition(Position.fromDegrees(42.3333, -71.0833));
		Moon moon = o.getMoon();
		testDistSize("NH 1988-Jul-27", moon, 0.002454383735427, 1952.040);
	}
	

	public void testPlanetDistSize() throws AstroError {
		// From NASA Horizons:
		//	Jupiter 1988-Jul-27 00:30 UT
		//	Altitude = 11.7334°
		//	Dist 	 = 5.36552671511285 AU
		//	Diam 	 = 36.743 s
		Observation o = new Observation();
		o.setTime(new Instant(1988, 7, 27, 0, 30, 0));
		o.setObserverPosition(Position.fromDegrees(42.3333, -71.0833));
		Planet jupiter = o.getPlanet(Planet.Name.JUPITER);
		testDistSize("NH 1988-Jul-27", jupiter, 5.36552671511285, 36.743);
	}
	
	 
	// ******************************************************************** //
	// Magnitude.
	// ******************************************************************** //

	private static void testMagnitude(String lab, Body b, double check)
		throws AstroError
	{
		double M = b.get(Field.MAGNITUDE);
		
		assertTolerance(lab + " mag", M, check, 0.1);
	}
	
	
	public void testPlanetMagnitude() throws AstroError {
		// From NASA Horizons:
		//	Venus 1987-Apr-10 19:21 UT
		//	Mag 	 = -3.96
		Observation o = new Observation();
		o.setTime(new Instant(1987, 4, 10, 19, 21, 0));
		o.setObserverPosition(Position.fromDegrees(38.9213889, -77.0655556));
		Planet venus = o.getPlanet(Planet.Name.VENUS);
		testMagnitude("NH 1988-Jul-27", venus, -3.96);
	}

}

