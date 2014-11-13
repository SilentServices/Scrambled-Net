
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

import org.hermit.astro.Observation;


/**
 * Test code.
 */
public class TestObliquity
	extends TestCase
{
 
	// ******************************************************************** //
	// Test Code.
	// ******************************************************************** //

	private static void testMeanObliq(Observation o, double check) {
		double e = Math.toDegrees(o.get(Observation.OField.MEAN_OBLIQUITY));
		assertTolerance("mean obliquity", e, check, 0.000001);
	}


	public void testMeanObliq() {
		Observation o = new Observation();
		o.setDate(1979, 12, 31.0);
		testMeanObliq(o, 23.441893);
	}


	private static void testTrueObliq(Observation o, double check) {
		double e = Math.toDegrees(o.get(Observation.OField.TRUE_OBLIQUITY));
		assertTolerance("true obliquity", e, check, 0.000001);
	}


	public void testTrueObliq() {
		Observation o = new Observation();
		o.setDate(1987, 04, 10.0);
		testTrueObliq(o, 23.4435694);
	}

}

