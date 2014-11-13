
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
import org.hermit.astro.AstroConstants;
import org.hermit.astro.Body;
import org.hermit.astro.Instant;
import org.hermit.astro.Moon;
import org.hermit.astro.Observation;
import org.hermit.astro.Planet;
import org.hermit.astro.Sun;
import org.hermit.astro.Body.Field;
import org.hermit.geo.Position;
import org.hermit.utils.Angle;


/**
 * Test code.
 */
public class TestPosition
	extends TestCase
{
 
	// ******************************************************************** //
	// Heliocentric Co-Ordinates.
	// ******************************************************************** //
	
	private static void testHeliocentric(String lab, Body b,
									     double cl, double cb, double cr)
		throws AstroError
	{
		double L = toDegrees(b.get(Field.HE_LONGITUDE));
		double B = toDegrees(b.get(Field.HE_LATITUDE));
		double R = b.get(Field.HE_RADIUS);
		
		assertDegrees(lab + " Lon", L, cl, 0.00001);
		assertTolerance(lab + " Lat", B, cb, 0.00001);
		assertTolerance(lab + " Rad", R, cr, 0.00001);
	}
	
	
	public void testVenusHe() throws AstroError {
		Observation o = new Observation();
		Body venus = o.getBody(Body.Name.VENUS);

		// Meeus chapter 33.
		o.setTime(Instant.fromTd(1992, 12, 20.0));
		testHeliocentric("Meeus 33", venus, 26.11428, -2.62070, 0.724603);
	}

	 
	// ******************************************************************** //
	// Equatorial Co-Ordinates.
	// ******************************************************************** //

	private static void testEquatorial(String lab, Body b,
									   double cα, double cδ, double cd)
		throws AstroError
	{
		double α = toDegrees(b.get(Field.RIGHT_ASCENSION_AP));
		double δ = toDegrees(b.get(Field.DECLINATION_AP));
		double d = b.get(Field.EARTH_DISTANCE);
		
		assertDegrees(lab + " α", α, cα, 0.0005);
		assertTolerance(lab + " δ", δ, cδ, 0.0005);
		assertTolerance(lab + " d", d, cd, 0.000051);
	}


	private static void testEquatorial(String lab, Body b,
									   int ah, int am, double as,
									   int dd, int dm, double ds,
									   double r)
		throws AstroError
	{
		Angle ra = Angle.fromRightAscension(ah, am, as);
		Angle dec = Angle.fromDegrees(dd, dm, ds);
		testEquatorial(lab, b, ra.getDegrees(), dec.getDegrees(), r);
	}


	public void testSunEq() throws AstroError {
		// From Meeus chapter 25.
		Observation o = new Observation(Instant.fromTd(1992, 10, 13.0));
		Sun sun = o.getSun();
		testEquatorial("Meeus 25", sun, 198.378178, -7.783871, 0.99760775);
	}


	public void testMoonEq() throws AstroError {
		// From Meeus chapter 47.
		Observation o = new Observation(Instant.fromTd(1992, 4, 12.0));
		Moon moon = o.getMoon();
		testEquatorial("Meeus 47", moon,
					   134.688470, 13.768368, 368405.6 / AstroConstants.AU);
	}


	public void testMercuryEq() throws AstroError {
		// From NASA Horizons:
		//  Mercury 1988-Nov-22 00:00 UT
		//	RA   =  15 29 04.27   = 232.267791667°
		//	Dec	 = -18 42 40.2    = -18.7111667°
		//	Dist = 1.41999975439192
		Observation o = new Observation(new Instant(1988, 11, 22));
		Planet mercury = o.getPlanet(Planet.Name.MERCURY);
		testEquatorial("NH 1988-Nov-22", mercury,
					   232.267791667, -18.7111667, 1.4199997543919);
	}


	public void testVenusEq() throws AstroError {
		// From NASA Horizons:
		// 	Venus 1992-Dec-20 00:00 TT
		// 	RA   =  21 04 41.45  = 316.172708333°
		// 	Dec  = -18 53 16.8   = -18.8880000°
		// 	Dist = 0.910947738674360 
		Observation o = new Observation(Instant.fromTd(1992, 12, 20.0));
		Planet venus = o.getPlanet(Planet.Name.VENUS);
		testEquatorial("NH 1992-Dec-20", venus,
					   316.172708, -18.8880000, 0.9109477);
	}


	public void testMarsEq() throws AstroError {
		// From NASA Horizons:
		//	Mars 2003-Aug-28 03:17 UT
		//	Geocentric
		//	RA   = 22 38 07.25
		//	Dec  = -15 46 15.9
		//	Dist = 0.372756755997547
		Instant instant = new Instant(2003, 8, 28.0 + (3.0+17.0/60.0)/24.0);
		Observation o = new Observation(instant);
		Planet mars = o.getPlanet(Planet.Name.MARS);
		testEquatorial("NH 2003-Aug-28", mars,
					   339.530208333, -15.7710833, 0.372756755997547);
		
		// From NASA Horizons:
		//	Mars 2007-Mar-14 03:17
		//	RA   = 20 59 59.06
		//	Dec  = -18 10 02.4
		//	Dist = 2.02010322
		o.setTime(new Instant(2007, 3, 14, 3, 17, 0));
		testEquatorial("NH 2007-Mar-14", mars,
				   	   20,59,59.06, -18,10,2.4, 2.02010322);
	}

	
	public void testJupiterEq() throws AstroError {
		// From NASA Horizons:
		//  Jupiter 1988-Nov-22 00:00 UT
		//	RA   =  03 57 10.50   = 59.293750000°
		//	Dec	 = +19 23 35.8    = 19.3932778°
		//	Dist = 4.03398209416213
		Observation o = new Observation(new Instant(1988, 11, 22));
		Planet jupiter = o.getPlanet(Planet.Name.JUPITER);
		testEquatorial("NH 1988-Nov-22", jupiter,
					   59.293750000, 19.3932778, 4.03398209416213);
	}
	
	
	public void testSaturnEq() throws AstroError {
		// From NASA Horizons:
		//  Saturn 2007-Mar-14 03:17 UT
		//	RA   =  09 28 41.52
		//	Dec	 = +16 17 31.7
		//	Dist = 8.35204965
		Observation o = new Observation(new Instant(2007, 3, 14, 3, 17, 0));
		Planet saturn = o.getPlanet(Planet.Name.SATURN);
		testEquatorial("NH 2007-Mar-14", saturn,
					   9,28,41.52, 16,17,31.7, 8.35204965);
	}
	
	
	public void testUranusEq() throws AstroError {
		// From NASA Horizons:
		//  Uranus 2007-Mar-14 03:17 UT
		//	RA   =  23 06 31.02
		//	Dec	 = -06 31 26.3
		//	Dist = 21.07300984
		Observation o = new Observation(new Instant(2007, 3, 14, 3, 17, 0));
		Planet uranus = o.getPlanet(Planet.Name.URANUS);
		testEquatorial("NH 2007-Mar-14", uranus,
					   23,6,31.02, -6,31,26.3, 21.07300984);
	}
	
	
	public void testNeptuneEq() throws AstroError {
		// From NASA Horizons:
		//  Neptune 2002-Sep-21 19:17 UT
		//	RA   =  23 06 31.02
		//	Dec	 = -06 31 26.3
		//	Dist = 29.43560887
		Observation o = new Observation(new Instant(2002, 9, 21, 19, 17, 0));
		Planet neptune = o.getPlanet(Planet.Name.NEPTUNE);
		testEquatorial("NH 2002-Sep-21", neptune,
					   20,43,17.57, -18,6,4.1, 29.43560887);
		
	}


	// ******************************************************************** //
	// Horizontal Co-Ordinates.
	// ******************************************************************** //

	private static void testHorizontal(String lab, Body b,
									   double cA, double ch)
		throws AstroError
	{
		double A = toDegrees(b.get(Field.LOCAL_AZIMUTH));
		double h = toDegrees(b.get(Field.LOCAL_ALTITUDE));

		assertDegrees(lab + " A", A, cA, 0.002);
		assertTolerance(lab + " h", h, ch, 0.002);
	}


	public void testVenusHoriz() throws AstroError {
		// From NASA Horizons:
		//	Venus 1987-Apr-10 19:21 UT
		//	Azimuth  = 248.0327
		//	Altitude = 15.1240
		Observation o = new Observation(new Instant(1987, 4, 10, 19, 21, 0));
		o.setObserverPosition(Position.fromDegrees(38.9213889, -77.0655556));
		Planet venus = o.getPlanet(Planet.Name.VENUS);
		testHorizontal("NH 1987-Apr-10", venus, 248.0327, 15.1240);

		// From Meeus chapter 13.
		o.setTime(new Instant(1987, 4, 10, 19, 21, 0));
		o.setObserverPosition(Position.fromDegrees(38.9213889, -77.0655556));
		testHorizontal("Meeus 13", venus, 68.0337 + 180.0, 15.12495);
	}

}

