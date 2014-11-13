
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
import org.hermit.astro.Observation;
import org.hermit.astro.Planet;
import org.hermit.astro.Body.Field;
import org.hermit.geo.Position;


/**
 * Test code.
 */
public class TestParallax
	extends TestCase
{
 
	// ******************************************************************** //
	// Test Code.
	// ******************************************************************** //

	private static void testParallax(Body b, double cα, double cδ) throws AstroError {
		String n = b.getId().toString();
		String lab = "Topo Position " + n;

//		double α = toDegrees(b.get(Field.RIGHT_ASCENSION_AP));
//		double δ = toDegrees(b.get(Field.DECLINATION_AP));
		double αt = toDegrees(b.get(Field.RIGHT_ASCENSION_TOPO));
		double δt = toDegrees(b.get(Field.DECLINATION_TOPO));
		
		assertDegrees(lab + " α", αt, cα, 0.001);
		assertTolerance(lab + " δ", δt, cδ, 0.001);
	}


	public void testParallax() throws AstroError {
		// From Meeus chapter 40.
		Instant instant = new Instant(2003, 8, 28.0 + (3.0+17.0/60.0)/24.0);
		Observation o = new Observation(instant);
		o.setObserverPosition(Position.fromDegrees(33.3561111, -116.8625));
		o.setObserverAltitude(1706);
		Planet mars = o.getPlanet(Planet.Name.MARS);
		testParallax(mars, 339.530208 + 0.0053917, -15.7750000);
	}
	
}

