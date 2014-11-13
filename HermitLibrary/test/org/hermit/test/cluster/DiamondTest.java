
/**
 * cluster: Tests of cluster analysis algorithms.
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */


package org.hermit.test.cluster;


import org.hermit.geometry.Point;


/**
 * Moderate Voronoi test scenario.  This class runs a moderate Voronoi test
 * on a diamond-pattern input, and does a fairly good check of the results.
 * 
 * The input and output data came from here:
 * http://people.sc.fsu.edu/~burkardt/c_src/sweep2/sweep2.html
 */
public class DiamondTest
    extends VoronoiTest
{

    // Input sites.
    private static final Point[] DIAMOND_SITES = {
        new Point(0.0, 0.0),
        new Point(0.0, 1.0),
        new Point(0.2, 0.5),
        new Point(0.3, 0.6),
        new Point(0.4, 0.5),
        new Point(0.6, 0.3),
        new Point(0.6, 0.5),
        new Point(1.0, 0.0),
        new Point(1.0, 1.0),
    };
    
    
    // Expected vertices.  { x, y } indicates a Voronoi vertex at
    // the point (x, y).
    private static final Point[] DIAMOND_VERTS = {
        new Point(0.500000, -0.250000),
        new Point(0.287500, 0.175000),
        new Point(0.300000, 0.200000),
        new Point(0.500000, 0.400000),
        new Point(0.300000, 0.500000),
        new Point(0.987500, 0.400000),
        new Point(0.500000, 0.700000),
        new Point(0.06428571429, 0.73571428571),
        new Point(1.112500, 0.500000),
        new Point(-0.525000, 0.500000),
        new Point(0.57631578947, 0.92894736842),
        new Point(0.500000, 1.062500),
        Point.INFINITE,
    };


    // Expected lines.  Each entry { a, b, c } indicates a line with the
    // equation a*x + b*y = c.  These lines are used in the description
    // of the edtges.
    private static final double[][] DIAMOND_LINES = {
        { 1.000000, 0.000000, 0.500000 },
        { 1.000000, -0.750000, 0.687500 },
        { 1.000000, 0.500000, 0.375000 },
        { 0.400000, 1.000000, 0.290000 },
        { 1.000000, -1.000000, 0.100000 },
        { 0.000000, 1.000000, 0.400000 },
        { 1.000000, -0.500000, 0.200000 },
        { 1.000000, 0.000000, 0.300000 },
        { 1.000000, 0.000000, 0.500000 },
        { 1.000000, 1.000000, 0.800000 },
        { -1.000000, 1.000000, 0.200000 },
        { -0.800000, 1.000000, -0.390000 },
        { 1.000000, -0.333333, 0.266667 },
        { -0.400000, 1.000000, 0.710000 },
        { 0.800000, 1.000000, 1.390000 },
        { -0.750000, 1.000000, 0.687500 },
        { 0.000000, 1.000000, 0.500000 },
        { 0.000000, 1.000000, 0.500000 },
        { 1.000000, 0.571429, 1.107143 },
        { 1.000000, 0.000000, 0.500000 },
    };
    

    // Expected edges.  Each entry { l, v1, v2 } indicates a Voronoi edge,
    // which is the segment of the line with index l that extends from
    // Voronoi vertex v1 to v2.  If either vertex has the value -1,
    // this indicates that the line segment is actually a semi-infinite
    // ray, which extends to infinity on that side.
    private static final int[][] DIAMOND_EDGES = {
        { 2, 1, 0 },
        { 6, 1, 2 },
        { 4, 2, 3 },
        { 7, 4, 2 },
        { 5, 3, 5 },
        { 1, 0, 5 },
        { 10, 4, 6 },
        { 8, 6, 3 },
        { 9, 7, 4 },
        { 11, 5, 8 },
        { 3, 9, 1 },
        { 13, 9, 7 },
        { 12, 6, 10 },
        { 14, 10, 8 },
        { 15, 7, 11 },
        { 18, 11, 10 },
        { 17, -1, 9 },
        { 19, -1, 11 },
        { 16, 8, -1 },
        { 0, 0, -1 },
    };


    /**
     * Run a test on the diamond test data.
     */
    public void testDiamond() {
        voronoiTest(DIAMOND_SITES, DIAMOND_VERTS, DIAMOND_LINES, DIAMOND_EDGES);
    }

}

