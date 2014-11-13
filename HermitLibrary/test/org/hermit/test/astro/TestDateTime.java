
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

import java.util.Calendar;
import java.util.TimeZone;

import junit.framework.TestCase;

import org.hermit.astro.Instant;
import org.hermit.astro.Observation;


/**
 * Test code.
 */
public class TestDateTime
	extends TestCase
{
	
	private static void testJulian(int y, int m, double d, double check) {
		String lab = String.format("%5d/%02d/%06.3f", y, m, d);
		
		Instant i = new Instant(y, m, d);
		double j = i.getUt();
		assertTolerance(lab + ": ymdToJulian", j, check, 0.0001);

		double[] ymd = i.getYmd();
		assertEquals(lab + ": julianToYmd y", (double) y, ymd[0]);
		assertEquals(lab + ": julianToYmd m", (double) m, ymd[1]);
		assertTolerance(lab + ": julianToYmd d", ymd[2], d, 0.000001);
	}
	
	
	public void testJulian() {
		testJulian(1970, 1, 1.0, Instant.JD_UNIX);
		testJulian(1985, 2, 17.25, 2446113.75);
		testJulian(1989, 12, 31.0, 2447891.5);
		testJulian(1979, 12, 31.0, 2444238.5);
		testJulian(1899, 12, 31.5, Instant.J1900);
		testJulian(1957, 10, 4.81, 2436116.31);
	}


	private static void testJavaJulian(int y, int m, double d, double check) {
		String lab = String.format("%5d/%02d/%06.3f", y, m, d);
		
		// Use Calendar to process the y/m/d.
		TimeZone UTC = TimeZone.getTimeZone("UTC");
		Calendar cal = Calendar.getInstance(UTC);
		int day = (int) d;
		double rem = (d - day) * 24.0;
		int hour = (int) rem;
		rem = (rem - hour) * 60;
		int min = (int) rem;
		rem = (rem - min) * 60;
		int sec = (int) rem;
		cal.set(y, m - 1, day, hour, min, sec);

		// Get the Java time in ms.  Make an Instant from it.
		long time = cal.getTimeInMillis();
		Instant i = new Instant(time);
		
		// Check that the Julian is right.
		double j = i.getUt();
		assertTolerance(lab + ": javaToJulian", j, check, 0.0001);
	}

	
	public void testJavaJulian() {
		testJavaJulian(1970, 1, 1.0, Instant.JD_UNIX);
		testJavaJulian(1985, 2, 17.25, 2446113.75);
		testJavaJulian(1989, 12, 31.0, 2447891.5);
		testJavaJulian(1979, 12, 31.0, 2444238.5);
		testJavaJulian(1899, 12, 31.5, Instant.J1900);
		testJavaJulian(1957, 10, 4.81, 2436116.31);
	}


	private static void testUtToGmst(int y, int m, int d, double UT, double check) {
		String lab = String.format("%5d/%02d/%02d", y, m, d);
		
		Observation o = new Observation();
		o.setDate(y, m, (double) d + UT / 24.0);

		double GMST = o.get(Observation.OField.GMST_INSTANT);
		assertTolerance(lab + ": Gmst", GMST, check, 0.000001);
	}
	
	
	public void testUtToGmst() {
		testUtToGmst(1980, 4, 22, 14.614353, 4.668119);
		testUtToGmst(1987, 4, 10, 0, 13.1795463333333);
		testUtToGmst(1987, 4, 10, 19.0 + 21.0/60.0, 8.0 + 34.0/60.0 + 57.0896 / 3600.0);
	}
	

	private static void testUtToGast(int y, int m, int d, double UT, double check) {
		String lab = String.format("%5d/%02d/%02d", y, m, d);
		
		Observation o = new Observation();
		o.setDate(y, m, (double) d + UT / 24.0);

		double GAST = o.get(Observation.OField.GAST_INSTANT);
		assertTolerance(lab + ": Gast", GAST, check, 0.000001);
		
		double GAST_M = o.get(Observation.OField.GAST_MIDNIGHT);
		assertTolerance(lab + ": Gast", GAST_M, check, 0.000001);
	}
	
	
	public void testUtToGast() {
		testUtToGast(1987, 4, 10, 0, 13.0 + 10.0/60.0 + 46.1351/3600.0);
		testUtToGast(1988, 3, 20, 0, 177.74208 / 15);
	}
	
}

