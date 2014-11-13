
/**
 * geo: geographical utilities.
 * <br>Copyright 2004-2009 Ian Cameron Smith
 *
 * <p>This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation (see COPYING).
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * General Public License for more details.
 *
 * <p>You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 */

package org.hermit.test.geo;


import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;
import static org.hermit.geo.GeoConstants.Ellipsoid.NAD27;
import static org.hermit.geo.GeoConstants.Ellipsoid.WGS84;
import static org.hermit.test.NumericAsserts.assertDegrees;
import static org.hermit.test.NumericAsserts.assertPercent;
import static org.hermit.test.NumericAsserts.assertRange;
import static org.hermit.test.NumericAsserts.assertTolerance;
import junit.framework.TestCase;

import org.hermit.geo.Azimuth;
import org.hermit.geo.Distance;
import org.hermit.geo.GeoCalculator;
import org.hermit.geo.Position;
import org.hermit.geo.Vector;
import org.hermit.geo.GeoConstants.Ellipsoid;


/**
 * Test geodetic calculations.
 *
 * @author	Ian Cameron Smith
 */
public class GeodeticTests
	extends TestCase
{

	// ******************************************************************** //
	// Test Definitions.
	// ******************************************************************** //

	private static final class TestData {
		public TestData(String name, Ellipsoid elip,
				int ad1, int am1, double as1,
				int nd1, int nm1, double ns1,
				int ad2, int am2, double as2,
				int nd2, int nm2, double ns2,
				int zd12, int zm12, double zs12, 
				int zd21, int zm21, double zs21,
				double dist)
		{
			testName = name;
			ellipsoid = elip;
			pos1 = new Position(dmsToRadians(ad1, am1, as1),
					dmsToRadians(nd1, nm1, ns1));
			pos2 = new Position(dmsToRadians(ad2, am2, as2),
					dmsToRadians(nd2, nm2, ns2));
			azimuth12 = new Azimuth(dmsToRadians(zd12, zm12, zs12));
			azimuth21 = new Azimuth(dmsToRadians(zd21, zm21, zs21));
			distance = new Distance(dist);
		}

		private static double dmsToRadians(int d, int m, double s) {
			// Any component can be negative to make it all negative.
			int sign = 1;
			if (d < 0) {
				sign = -1;
				d = -d;
			}
			if (m < 0) {
				sign = -1;
				m = -m;
			}
			if (s < 0) {
				sign = -1;
				s = -s;
			}
			double deg = d + m/60.0 + s/3600.0;
			return toRadians(deg) * sign;
		}

		// Name of the test.
		String testName;
		
		// Ellipsoid on which the test is defined.
		Ellipsoid ellipsoid;
		
		// Start and end stations.
		Position pos1;
		Position pos2;
		
		// Azimuth from 1 to 2, and from 2 to 1.
		Azimuth azimuth12;
		Azimuth azimuth21;

		// Distance between stations.
		Distance distance;
	}
	
	
	/**
	 * Table of test data.  Each row contains:
	 *   - Test name, ellipsoid to use
	 *   - Station 1 latitude, degrees; longitude, degrees
	 *   - Station 2 latitude, degrees; longitude, degrees
	 *   - Forward azimuth, degrees; back azimuth, degrees
	 *   - Distance, metres
	 */
	private static final TestData[] testData = {
		// Forward:
		//   1->2:
		//     33 22 11.54321 North    112 55 44.33333 West  
		//   2->1:
		//     34  0 12.12345 North    111  0 12.12345 West  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance =   191872.11903
		//    Forward azimuth      = 249  3 16.4237
		//    Back azimuth         =  67 59 11.1619
		//   2->1:
		//    Ellipsoidal distance =   191872.11903
		//    Forward azimuth      =  67 59 11.1619
		//    Back azimuth         = 249  3 16.4237
		new TestData("NGS Jones/Smith", WGS84,
						  34, 0,12.12345, -111, 0,12.12345,
						  33,22,11.54321, -112,55,44.33333,
						 249, 3,16.42370,   67,59,11.16190,
						 191872.1190
		),

		// Forward:
		//   1->2:
		//     44 33  0.00000 North     70 12 34.78900 West  
		//   2->1:
		//     45  0 12.00000 North     68  0  0.00000 West  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance =   182009.16792
		//    Forward azimuth      = 254 42 44.6439
		//    Back azimuth         =  73  9 21.3315
		//   2->1:
		//    Ellipsoidal distance =   182009.16792
		//    Forward azimuth      =  73  9 21.3315
		//    Back azimuth         = 254 42 44.6439
		new TestData("NGS Charlie/Sam", NAD27,
						  45, 0,12.00000,  -68, 0, 0.00000,
						  44,33, 0.00000,  -70,12,34.78900,
						 254,42,44.64390,   73, 9,21.33150,
						 182009.1679
		),

		// Forward:
		//   1->2:
		//     37 39 10.15612 South    143 55 35.38388 East  
		//   2->1:
		//     37 57  3.72030 South    144 25 29.52440 East  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance =    54972.27114
		//    Forward azimuth      = 306 52  5.3731
		//    Back azimuth         = 127 10 25.0703
		//   2->1:
		//    Ellipsoidal distance =    54972.27114
		//    Forward azimuth      = 127 10 25.0703
		//    Back azimuth         = 306 52  5.3731
		new TestData("Geoscience Australia", WGS84,
						 -37,57,03.72030, 144,25,29.52440,
						 -37,39,10.15610, 143,55,35.38390,
						 306,52,05.37,    127,10,25.07,
						 54972.271
		),

		// Forward:
		//   1->2:
		//     37 39 10.15612 South    143 55 35.38388 East  
		//   2->1:
		//     37 57  3.72030 South    144 25 29.52440 East  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance =    54972.27114
		//    Forward azimuth      = 306 52  5.3731
		//    Back azimuth         = 127 10 25.0703
		//   2->1:
		//    Ellipsoidal distance =    54972.27114
		//    Forward azimuth      = 127 10 25.0703
		//    Back azimuth         = 306 52  5.3731
		new TestData("Meeus sect. 11", WGS84,
						  48,50,11.00000,   2,20,14.00000,
						  38,55,17.00000, -77,03,56.00000,
						 291,50,01.08851,  51,47,36.87089,
						 6181631.68521
		),
					     
		// Forward:
		//   1->2:
		//      4 33  0.00000 North     68  0  0.00000 West  
		//   2->1:
		//     45  0 12.00000 North     68  0  0.00000 West  
		// Inverse:
		//   1->2:
		//  longitudal difference is near zero 
		//    Ellipsoidal distance =  4482191.25533
		//    Forward azimuth      = 180  0  0.0000
		//    Back azimuth         =   0  0  0.0000
		//   2->1:
		//  longitudal difference is near zero 
		//    Ellipsoidal distance =  4482191.25533
		//    Forward azimuth      =   0  0  0.0000
		//    Back azimuth         = 180  0  0.0000
		new TestData("Meridional", WGS84,
						  45, 0,12.00000,  -68, 0, 0.00000,
						   4,33, 0.00000,  -68, 0, 0.00000,
						 180, 0, 0.00000,    0, 0, 0.00000,
						 4482191.25533
		),

		// Forward:
		//   1->2:
		//      4 33  0.00000 North     68  0  0.00000 West  
		//   2->1:
		//     45  0 12.00000 North     68  0  0.00000 West  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance =  4482191.25533
		//    Forward azimuth      = 180  0  0.0000
		//    Back azimuth         =   0  0  0.0000
		//   2->1:
		//    Ellipsoidal distance =  4482191.25533
		//    Forward azimuth      =   0  0  0.0000
		//    Back azimuth         = 180  0  0.0000
		new TestData("Near-Meridional", WGS84,
						  45, 0,12.00000,  -68, 0, 0.00000,
						   4,33, 0.00000,  -68, 0, 0.000001,
						 180, 0, 0.00000,    0, 0, 0.00000,
						 4482191.25533
		),

		// Forward:
		//   1->2:
		//      0  0  0.00000 North     23  0  0.00000 East  
		//   2->1:
		//      0  0  0.00000 South     68  0  0.00000 West  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance = 10130073.66219
		//    Forward azimuth      =  90  0  0.0000
		//    Back azimuth         = 270  0  0.0000
		//   2->1:
		//    Ellipsoidal distance = 10130073.66219
		//    Forward azimuth      = 270  0  0.0000
		//    Back azimuth         =  90  0  0.0000
		new TestData("Equatorial", WGS84,
						   0, 0, 0.00000,  -68, 0, 0.00000,
						   0, 0, 0.00000,   23, 0, 0.00000,
						  90, 0, 0.00000,  270, 0, 0.00000,
						 10130073.66219
		),

		// Forward:
		//   1->2:
		//      0  0  0.00000 South     23  0  0.00000 East  
		//   2->1:
		//      0  0  0.00000 North     68  0  0.00000 West  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance = 10130073.66219
		//    Forward azimuth      =  90  0  0.0000
		//    Back azimuth         = 270  0  0.0000
		//   2->1:
		//    Ellipsoidal distance = 10130073.66219
		//    Forward azimuth      = 270  0  0.0000
		//    Back azimuth         =  90  0  0.0000
		new TestData("Near-Equatorial", WGS84,
						   0, 0, 0.000001, -68, 0, 0.00000,
						   0, 0,-0.000001,  23, 0, 0.00000,
						  90, 0, 0.00000,  270, 0, 0.00000,
						 10130073.66219
		),
		
		// Forward:
		//   1->2:
		//      0 25  0.00000 South    178 47 49.39063 West  
		//   2->1:
		//      0 25  0.00000 North      1 12 10.60937 West  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance = 19903598.64278
		//    Forward azimuth      = 269 59 52.1531
		//    Back azimuth         =  89 59 52.1531
		//   2->1:
		//    Ellipsoidal distance = 19903598.64278
		//    Forward azimuth      =  89 59 52.1531
		//    Back azimuth         = 269 59 52.1531
		new TestData("Near-anti-nodal", WGS84,
						   0, 25,0.00000,   0, 0, 0.00000,
						   0,-25,0.00000, 180, 0, 0.00000,
						 269,59,52.15310,  89,59,52.15310,
						 19903598.64278
		),

		// Forward:
		//   1->2:
		//      0  0  0.00000 South    180  0  0.00000 West  
		//   2->1:
		//      0  0  0.00000 North      0  0  0.00000 East  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance =        0.00000
		//    Forward azimuth      =   0  0  0.0000
		//    Back azimuth         =   0  0  0.0000
		//   2->1:
		//    Ellipsoidal distance =        0.00000
		//    Forward azimuth      =   0  0  0.0000
		//    Back azimuth         =   0  0  0.0000
		new TestData("Very near-anti-nodal", WGS84,
						   0, 0, 0.000001,   0, 0, 0.00000,
						   0, 0,-0.000001, 180, 0, 0.00000,
						   0, 0, 0.00000,    0, 0, 0.00000,
						 20003931.45846
		),

		// Forward:
		//	   1->2:
		//	      0 25  0.00000 North    180  0  0.00000 West  
		//	   2->1:
		//	      0  0  0.00000 North      0  0  0.00000 East  
		//	 Inverse:
		//	   1->2:
		//	    Ellipsoidal distance =        0.00000
		//	    Forward azimuth      =   0  0  0.0000
		//	    Back azimuth         =   0  0  0.0000
		//	   2->1:
		//	    Ellipsoidal distance =        0.00000
		//	    Forward azimuth      =   0  0  0.0000
		//	    Back azimuth         =   0  0  0.0000
		new TestData("Near-semi-anti-nodal", WGS84,
						   0,25, 0.00000,    0, 0, 0.00000,
						   0, 0,-0.000001, 180, 0, 0.00000,
						   0, 0, 0.00000,    0, 0, 0.00000,
						 19957858.83541
		),

		// Forward:
		//	   1->2:
		//	      0 25  0.00000 North    180  0  0.00000 West  
		//	   2->1:
		//	      0  0  0.00000 South      0  0  0.00000 East  
		//	 Inverse:
		//	   1->2:
		//	    Ellipsoidal distance = 19957858.83538
		//	    Forward azimuth      = 180  0  0.0000
		//	    Back azimuth         = 180  0  0.0000
		//	   2->1:
		//	    Ellipsoidal distance = 19957858.83538
		//	    Forward azimuth      = 180  0  0.0000
		//	    Back azimuth         = 180  0  0.0000
		new TestData("Semi-anti-nodal", WGS84,
						   0,25, 0.00000,    0, 0, 0.00000,
						   0, 0,-0.00000,  180, 0, 0.00000,
						   0, 0, 0.00000,    0, 0, 0.00000,
						 19957858.83538
		),

		// Forward:
		//   1->2:
		//      0  0  0.00000 South    180  0  0.00000 West  
		//   2->1:
		//      0  0  0.00000 South      0  0  0.00000 East  
		// Inverse:
		//   1->2:
		//    Ellipsoidal distance = 20003931.45846
		//    Forward azimuth      =   0  0  0.0000
		//    Back azimuth         =   0  0  0.0000
		//   2->1:
		//    Ellipsoidal distance = 20003931.45846
		//    Forward azimuth      =   0  0  0.0000
		//    Back azimuth         =   0  0  0.0000
		new TestData("Anti-nodal", WGS84,
						   0, 0, 0.00000,    0, 0, 0.00000,
						   0, 0,-0.00000,  180, 0, 0.00000,
						   0, 0, 0.00000,    0, 0, 0.00000,
						 20003931.45846
		),
	};


	// ******************************************************************** //
	// Test Code.
	// ******************************************************************** //

	private void doOffset(String msg, TestData test, double tol) {
		msg = msg + ": " + test.testName;
		Position p1 = test.pos1;
		Position p2 = test.pos2;
		double llTol = 360.0 * tol / 100.0;
		if (test.testName.equals("Near-anti-nodal"))
			llTol = 1.5;

		// Try getting from 1 to 2.
		Position efrom1 = p1.offset(new Vector(test.distance, test.azimuth12));
		assertTolerance(msg + ": forward lat", efrom1.getLatDegs(), p2.getLatDegs(), llTol);
		assertTolerance(msg + ": forward lon", efrom1.getLonDegs(), p2.getLonDegs(), llTol);
		
		// Try getting from 2 to 1.
		Position efrom2 = p2.offset(new Vector(test.distance, test.azimuth21));
		assertTolerance(msg + ": reverse lat", efrom2.getLatDegs(), p1.getLatDegs(), llTol);
		assertTolerance(msg + ": reverse lon", efrom2.getLonDegs(), p1.getLonDegs(), llTol);
	}
	

	private void doDistance(String msg, TestData test, double tol) {
		msg = msg + ": " + test.testName;
		Position p1 = test.pos1;
		Position p2 = test.pos2;
		if (test.testName.endsWith("ti-nodal"))
			tol = Math.max(tol, 1);
		
		// Getting the distance and azimuth from 1 to 2.
		Distance d1 = p1.distance(p2);
		assertPercent(msg + ": forward dst", d1.getMetres(), test.distance.getMetres(), tol);

		// Getting the distance from 2 to 1.
		Distance d2 = p2.distance(p1);
		assertPercent(msg + ": reverse dst", d2.getMetres(), test.distance.getMetres(), tol);
	}


    private void doLatDistance(String msg, TestData test, double tol) {
        msg = msg + ": " + test.testName;
        Position p1 = test.pos1;
        Position p2 = test.pos2;
        
        // Getting the distance from 1 to the latitude of 2.  We will
        // calculate the same distance with the regular method as our
        // reference.
        Position ref1 = new Position(p2.getLatRads(), p1.getLonRads());
        Distance r1 = p1.distance(ref1);
        Distance d1 = p1.latDistance(p2.getLatRads());
        assertPercent(msg + ": forward lat dst", d1.getMetres(), r1.getMetres(), tol);

        // Getting the distance from 2 to the latitude of 1.
        Position ref2 = new Position(p1.getLatRads(), p2.getLonRads());
        Distance r2 = p2.distance(ref2);
        Distance d2 = p2.latDistance(p1.getLatRads());
        assertPercent(msg + ": reverse lat dst", d2.getMetres(), r2.getMetres(), tol);
    }
    

	private void doVector(String msg, TestData test, double tol) {
		msg = msg + ": " + test.testName;
		Position p1 = test.pos1;
		Position p2 = test.pos2;
		double azTol = 360.0 * tol / 100.0;
		if (GeoCalculator.getCurrentAlgorithm() == GeoCalculator.Algorithm.HAVERSINE) {
			if (test.testName.equals("Very near-anti-nodal"))
				azTol = 91.0;
			else if (test.testName.equals("Anti-nodal"))
				azTol = 360.0;
		}

		// Getting the distance and azimuth from 1 to 2.
		Vector vec1 = p1.vector(p2);
		assertPercent(msg + ": forward dst", vec1.getDistanceMetres(), test.distance.getMetres(), tol);
		assertDegrees(msg + ": forward azi", vec1.getAzimuthDegrees(), test.azimuth12.getDegrees(), azTol);

		// Getting the distance from 2 to 1.
		Vector vec2 = p2.vector(p1);
		assertPercent(msg + ": reverse dst", vec2.getDistanceMetres(), test.distance.getMetres(), tol);
		assertDegrees(msg + ": reverse azi", vec2.getAzimuthDegrees(), test.azimuth21.getDegrees(), azTol);
	}


	// ******************************************************************** //
	// Tests.
	// ******************************************************************** //

	public void testHaversineOffset() {
		GeoCalculator.setAlgorithm(GeoCalculator.Algorithm.HAVERSINE);
		for (TestData test : testData)
			doOffset("Haversine offset", test, 0.6);
	}
	
    
    public void testHaversineVector() {
        GeoCalculator.setAlgorithm(GeoCalculator.Algorithm.HAVERSINE);
        for (TestData test : testData)
            doVector("Haversine vector", test, 0.6);
    }
    

    public void testHaversineLatDistance() {
        GeoCalculator.setAlgorithm(GeoCalculator.Algorithm.HAVERSINE);
        for (TestData test : testData)
            doLatDistance("Haversine lat distance", test, 0.001);
    }
    

	public void testVincentyOffset() {
		for (TestData test : testData) {
			GeoCalculator.setAlgorithm(GeoCalculator.Algorithm.VINCENTY, test.ellipsoid);
			doOffset("Vincenty offset", test, 0.00001);
		}		
	}
	

	public void testAndoyerDistance() {
		for (TestData test : testData) {
			GeoCalculator.setAlgorithm(GeoCalculator.Algorithm.ANDOYER, test.ellipsoid);
			doDistance("Andoyer distance", test, 0.001);
		}		
	}
	

	public void testVincentyVector() {
		for (TestData test : testData) {
			GeoCalculator.setAlgorithm(GeoCalculator.Algorithm.VINCENTY, test.ellipsoid);
			doVector("Vincenty vector", test, 0.00001);
		}		
	}
	

	public void testGeocentricLat() {
		// Aside from the 45 degrees case, this is a weak test, but at
		// least it tests for sane results.  The 45 degrees case is a good
		// one, from Meeus.
		for (int lat = 0; lat <= 90; lat += 5) {
			Position pos = Position.fromDegrees(lat, 0);
			double gLat = toDegrees(pos.getGeocentricLat());
			double delta = lat - gLat;
			assertRange("Geo lat " + lat, delta, 0, 0.9125);
			
			if (lat == 45)
				assertTolerance("Geo lat " + lat, delta, 0.192425, 0.000001);
		}		
	}
	
}

