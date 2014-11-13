
/**
 * test: test code.
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

import org.hermit.geo.PointOfInterest;
import org.hermit.geo.Position;


/**
 * Test the PointOfInterest utils.
 */
public class PoiTest {

    // ******************************************************************** //
    // Main.
    // ******************************************************************** //
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		for (double latitude : LATS) {
			for (double offset : OFFSETS) {
				double lat = latitude + offset;
				if (lat > HALFPI || lat < -HALFPI)
					continue;
				Position pos = new Position(lat, 12.34);
				double deg = Math.toDegrees(lat);
				String desc = PointOfInterest.describePosition(pos);
				System.out.format("%8.3f -> %s\n", deg, desc);
			}
		}
		
		System.out.println();

		for (double[] latlon : POINTS) {
			for (double offset : OFFSETS) {
				double lat = latlon[0] + offset;
				double lon = latlon[1] + offset;
				if (lat > HALFPI || lat < -HALFPI)
					continue;
				if (lon > Math.PI || lon < -Math.PI)
					continue;
				Position pos = new Position(lat, lon);
				double latd = Math.toDegrees(lat);
				double lond = Math.toDegrees(lon);
				String desc = PointOfInterest.describePosition(pos);
				System.out.format("%8.3f,%8.3f -> %s\n", latd, lond, desc);
			}
		}
	}
	
	
    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //
	
	// Half pi = 90 degrees.
    private static final double HALFPI = Math.PI / 2.0;

    // The Earth's axial tilt in radians (approx).
    private static final double EARTH_TILT = Math.toRadians(23.43944444);

    // Interesting latitudes.  Don't expose these as public constants
    // because they are not constant; we need to calculate the correct
    // axial tilt.
    private static final double NPOLE = HALFPI;
    private static final double ARCTIC = HALFPI - EARTH_TILT;
    private static final double CANCER = EARTH_TILT;
    private static final double EQUATOR = 0.0;
    private static final double CAPRICORN = -EARTH_TILT;
    private static final double ANTARC = -HALFPI + EARTH_TILT;
    private static final double SPOLE = -HALFPI;

	// One nautical mile over the Earth's surface, in radians.
	private static final double NMILE = Math.toRadians(1.0 / 60.0);
	
	// Table of offsets used to generate test positions.
	private static final double[] OFFSETS = {
		NMILE * 230, NMILE * 115.67, NMILE * 3.8765,
		NMILE / 4.72, NMILE * 0.0323, NMILE * 0.0093,
		0,
		-NMILE * 0.0093, -NMILE * 0.0323, -NMILE / 4.72,
		-NMILE * 3.8765, -NMILE * 115.67, -NMILE * 230,
	};
	
	// Table of test latitudes.
	private static final double[] LATS = {
		NPOLE, ARCTIC, CANCER, EQUATOR, CAPRICORN,
		Math.toRadians(-40), Math.toRadians(-50), ANTARC, SPOLE
	};
	
	// Table of test latitudes.
	private static final double[][] POINTS = {
		{ -Math.toRadians(47d + 9d/60d), -Math.toRadians(126d + 43d/60d) },
	};

}

