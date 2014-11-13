
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

package org.hermit.test.astro;


import static org.hermit.test.NumericAsserts.assertTolerance;
import junit.framework.TestCase;

import org.hermit.astro.Instant;


/**
 * Test astro methods.
 *
 * @author	Ian Cameron Smith
 */
public class TestDeltaT
	extends TestCase
{

	// ******************************************************************** //
	// Test Definitions.
	// ******************************************************************** //

	private static final class TestData {
		public TestData(int year, double dt, double tol) {
			this.year = year;
			this.deltaT = dt;
			this.tolerance = tol;
		}

		final int year;
		final double deltaT;
		final double tolerance;
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
	    new TestData(1650, +46, 4.2),
	    new TestData(1652, +44, 4),
	    new TestData(1654, 42, 2.5),
	    new TestData(1656, 40, 2.0),
	    new TestData(1658, 38, 2.0),
	    new TestData(1660, +35, 2.0),
	    new TestData(1662, 33, 2.0),
	    new TestData(1664, 31, 2.0),
	    new TestData(1666, 29, 2.0),
	    new TestData(1668, 26, 2.0),
	    new TestData(1670, +24, 2.0),
	    new TestData(1672, 22, 2.0),
	    new TestData(1674, 20, 2.0),
	    new TestData(1676, 18, 2.0),
	    new TestData(1678, 16, 2.0),
	    new TestData(1680, +14, 2.0),
	    new TestData(1682, 12, 2.0),
	    new TestData(1684, 11, 2.0),
	    new TestData(1686, 10, 2.0),
	    new TestData(1688, 9, 2.0),
	    new TestData(1690, +8, 2.0),
	    new TestData(1692, 7, 2.4),
	    new TestData(1694, 7, 2.0),
	    new TestData(1696, 7, 2.0),
	    new TestData(1698, 7, 2.0),
	    new TestData(1700, +7, 1.9),
	    new TestData(1702, 7, 2.2),
	    new TestData(1704, 8, 1.75),
	    new TestData(1706, 8, 1.75),
	    new TestData(1708, 9, 1.75),
	    new TestData(1710, +9, 1.75),
	    new TestData(1712, 9, 1.75),
	    new TestData(1714, 9, 1.75),
	    new TestData(1716, 9, 1.75),
	    new TestData(1718, 10, 1.75),
	    new TestData(1720, +10, 1.75),
	    new TestData(1722, 10, 1.75),
	    new TestData(1724, 10, 1.75),
	    new TestData(1726, 10, 1.75),
	    new TestData(1728, 10, 1.75),
	    new TestData(1730, 10, 1.75),
	    new TestData(1732, 10, 1.75),
	    new TestData(1734, 11, 1.75),
	    new TestData(1736, 11, 1.75),
	    new TestData(1738, 11, 1.75),
	    new TestData(1740, +11, 1.75),
	    new TestData(1742, 11, 1.75),
	    new TestData(1744, 12, 1.75),
	    new TestData(1746, 12, 1.75),
	    new TestData(1748, 12, 1.75),
	    new TestData(1750, +12, 1.75),
	    new TestData(1752, 13, 1.75),
	    new TestData(1754, 13, 1.75),
	    new TestData(1756, 13, 1.75),
	    new TestData(1758, 14, 1.75),
	    new TestData(1760, +14, 1.75),
	    new TestData(1762, 14, 1.75),
	    new TestData(1764, 14, 1.75),
	    new TestData(1766, 15, 1.75),
	    new TestData(1768, 15, 1.75),
	    new TestData(1770, +15, 1.75),
	    new TestData(1772, 15, 1.75),
	    new TestData(1774, 15, 1.75),
	    new TestData(1776, 16, 1.75),
	    new TestData(1778, 16, 1.75),
	    new TestData(1780, +16, 1.75),
	    new TestData(1782, 16, 1.75),
	    new TestData(1784, 16, 1.75),
	    new TestData(1786, 16, 1.75),
	    new TestData(1788, 16, 1.75),
	    new TestData(1790, +16, 1.75),
	    new TestData(1792, 15, 1.75),
	    new TestData(1794, 15, 1.75),
	    new TestData(1796, 14, 1.75),
	    new TestData(1798, 13, 1.75),
	    new TestData(1800, +13.1, 0.75),
	    new TestData(1802, 12.5, 0.75),
	    new TestData(1804, 12.2, 0.75),
	    new TestData(1806, 12.0, 0.75),
	    new TestData(1808, 12.0, 0.75),
	    new TestData(1810, +12.0, 0.75),
	    new TestData(1812, 12.0, 0.75),
	    new TestData(1814, 12.0, 0.75),
	    new TestData(1816, 12.0, 0.75),
	    new TestData(1818, 11.9, 0.75),
	    new TestData(1820, +11.6, 0.75),
	    new TestData(1822, 11.0, 0.75),
	    new TestData(1824, 10.2, 0.75),
	    new TestData(1826, 9.2, 0.75),
	    new TestData(1828, 8.2, 0.75),
	    new TestData(1830, +7.1, 0.75),
	    new TestData(1832, 6.2, 0.75),
	    new TestData(1834, 5.6, 0.75),
	    new TestData(1836, 5.4, 0.75),
	    new TestData(1838, 5.3, 0.75),
	    new TestData(1840, +5.4, 0.75),
	    new TestData(1842, 5.6, 0.75),
	    new TestData(1844, 5.9, 0.75),
	    new TestData(1846, 6.2, 0.75),
	    new TestData(1848, 6.5, 0.75),
	    new TestData(1850, +6.8, 0.75),
	    new TestData(1852, 7.1, 0.75),
	    new TestData(1854, 7.3, 0.75),
	    new TestData(1856, 7.5, 0.75),
	    new TestData(1858, 7.6, 0.75),
	    new TestData(1860, +7.7, 0.75),
	    new TestData(1862, 7.3, 0.75),
	    new TestData(1864, 6.2, 0.75),
	    new TestData(1866, 5.2, 0.75),
	    new TestData(1868, 2.7, 0.75),
	    new TestData(1870, +1.4, 0.75),
	    new TestData(1872, -1.2, 0.75),
	    new TestData(1874, -2.8, 0.75),
	    new TestData(1876, -3.8, 0.75),
	    new TestData(1878, -4.8, 0.75),
	    new TestData(1880, -5.5, 0.75),
	    new TestData(1882, -5.3, 0.75),
	    new TestData(1884, -5.6, 0.75),
	    new TestData(1886, -5.7, 0.75),
	    new TestData(1888, -5.9, 0.75),
	    new TestData(1890, -6.0, 0.75),
	    new TestData(1892, -6.3, 0.75),
	    new TestData(1894, -6.5, 0.75),
	    new TestData(1896, -6.2, 0.75),
	    new TestData(1898, -4.7, 0.75),
	    new TestData(1900, -2.8, 0.21),
	    new TestData(1902, -0.1, 0.21),
	    new TestData(1904, +2.6, 0.21),
	    new TestData(1906, 5.3, 0.21),
	    new TestData(1908, 7.7, 0.21),
	    new TestData(1910, +10.4, 0.21),
	    new TestData(1912, 13.3, 0.21),
	    new TestData(1914, 16.0, 0.21),
	    new TestData(1916, 18.2, 0.21),
	    new TestData(1918, 20.2, 0.21),
	    new TestData(1920, +21.1, 0.21),
	    new TestData(1922, 22.4, 0.21),
	    new TestData(1924, 23.5, 0.21),
	    new TestData(1926, 23.8, 0.21),
	    new TestData(1928, 24.3, 0.21),
	    new TestData(1930, +24.0, 0.21),
	    new TestData(1932, 23.9, 0.21),
	    new TestData(1934, 23.9, 0.21),
	    new TestData(1936, 23.7, 0.21),
	    new TestData(1938, 24.0, 0.21),
	    new TestData(1940, +24.3, 0.21),
	    new TestData(1942, 25.3, 0.21),
	    new TestData(1944, 26.2, 0.21),
	    new TestData(1946, 27.3, 0.14),
	    new TestData(1948, 28.2, 0.14),
	    new TestData(1950, +29.1, 0.14),
	    new TestData(1952, 30.0, 0.14),
	    new TestData(1954, 30.7, 0.14),
	    new TestData(1956, 31.4, 0.14),
	    new TestData(1958, 32.2, 0.14),
	    new TestData(1960, +33.1, 0.14),
	    new TestData(1962, 34.0, 0.14),
	    new TestData(1964, 35.0, 0.14),
	    new TestData(1966, 36.5, 0.14),
	    new TestData(1968, 38.3, 0.14),
	    new TestData(1970, +40.2, 0.14),
	    new TestData(1972, 42.2, 0.14),
	    new TestData(1974, 44.5, 0.12),
	    new TestData(1976, 46.5, 0.1),
	    new TestData(1978, 48.5, 0.1),
	    new TestData(1980, +50.5, 0.1),
	    new TestData(1982, 52.2, 0.1),
	    new TestData(1984, 53.8, 0.1),
	    new TestData(1986, 54.9, 0.1),
	    new TestData(1988, 55.8, 0.1),
	    new TestData(1990, +56.9, 0.1),
	    new TestData(1992, 58.3, 0.1),
	    new TestData(1994, 60.0, 0.1),
	    new TestData(1996, 61.6, 0.1),
	    new TestData(1998, 63.0, 0.1),
	    new TestData(2000, +63.8, 0.1),
	    new TestData(2002, 64.3, 0.1),
	    new TestData(2004, 64.6, 0.1),
	    
	    // Selected fixed dates.
	    new TestData(1500, 198.28, 0.1),
	    new TestData(1800, 13.7, 0.1),
	    new TestData(1900, -2.73, 0.1),
	    new TestData(2000, 63.874, 0.015),
	    new TestData(2009, 66.33, 0.06),
	    new TestData(2017, 70.03, 0.06),
	};
	

	// ******************************************************************** //
	// Tests.
	// ******************************************************************** //

	private void doYear(TestData test) {
		int year = test.year;
		double month = 0.0;
		double dt = Instant.calculateDeltaT(year, month);
		// double err = dt - test.deltaT;
		// String ok = Math.abs(err) < test.tolerance ? "OK" : "BAD";
		// System.out.format("%04d: got %8.4f want %8.4f  err %8.3f : %s\n",
		//                   year, dt, test.deltaT, err, ok);
		assertTolerance("" + year, dt, test.deltaT, test.tolerance);
	}


	public void testDeltaT() {
		for (TestData test : testData)
			doYear(test);
	}

}

