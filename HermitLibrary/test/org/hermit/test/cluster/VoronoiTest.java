
/**
 * cluster: Tests of cluster analysis algorithms.
 *
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 */


package org.hermit.test.cluster;


import java.util.Arrays;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.hermit.geometry.Edge;
import org.hermit.geometry.Point;
import org.hermit.geometry.Graph;
import org.hermit.geometry.voronoi.Fortune;


/**
 * Base class for tests of Voronoi diagram generation.
 */
public abstract class VoronoiTest
    extends TestCase
{

    /**
     * Run a Voronoi test.
     * 
     * @param   sites       Array of input sites.
     * @param   verts       Array of expected vertices.
     * @param   lines       Array of expected lines.  These aren't the line
     *                      segments of the final diagram; each entry is
     *                      of the form { a, b, c }, indicating a line with
     *                      the equation a*x + b*y = c.  These lines are
     *                      used in the description of the edges.
     * @param   edges       Array of expected edges.  Each entry is of the
     *                      form { l, v1, v2 }, and indicates a Voronoi edge,
     *                      which is the segment of the line with
     *                      index l that extends from Voronoi vertex v1 to v2.
     *                      If either vertex has the value -1, this indicates
     *                      that the line segment is actually a semi-infinite
     *                      ray, which extends to infinity on that side.
     */
    public static void voronoiTest(Point[] sites, Point[] verts, double[][] lines, int[][] edges) {
        // Compute the Voronoi graph based on the test sites.
        Graph graph = Fortune.ComputeVoronoiGraph(sites);
        //        graph.dump("");
        
        // Copy and sort the expected vertices array.  We can NOT sort
        // the original array, as the other arrays refer into it by index.
        Point[] refPoints = new Point[verts.length];
        for (int i = 0; i < verts.length; ++i)
            refPoints[i] = verts[i];
        Arrays.sort(refPoints);

        // Convert the generated vertices to an array, and sort
        // for comparison.
        Point[] vPoints = graph.getVertexArray();
        Arrays.sort(vPoints);
        
        // Compare the vertices.  Assume infinities to be equal.
        assertEquals("number of vertices", refPoints.length, vPoints.length);
        for (int i = 0; i < refPoints.length; ++i)
            if (!(refPoints[i].isInfinite() && vPoints[i].isInfinite()))
                assertEquals("vertex " + i, refPoints[i], vPoints[i]);
        
        // Convert the expected edges to an array.  Sort for comparison.
        Edge[] refEdges = new Edge[edges.length];
        for (int i = 0; i < edges.length; ++i) {
            int[] d = edges[i];
            Point v1 = d[1] >= 0 ? verts[d[1]] : Point.INFINITE;
            Point v2 = d[2] >= 0 ? verts[d[2]] : Point.INFINITE;
            
            // TODO: we're making up fake datum points here.  Should do this
            // based on the reference line equation.
            refEdges[i] = new Edge(v1, v2, new Point(0, 0), new Point(0, 1));
        }
        Arrays.sort(refEdges);

        // Convert the generated edges to an array and sort.
        Edge[] vEdges = graph.getEdgeArray();
        Arrays.sort(vEdges);
        
        // Compare the non-infinite edges.  Note that we're only checking
        // the position of the vertices as yet; this is not a complete
        // check in the case of semi-infinite or infinite edges.
        assertEquals("number of edges", refEdges.length, vEdges.length);
        for (int i = 0; i < refEdges.length; ++i)
            if (refEdges[i].compareTo(vEdges[i]) != 0)
                Assert.fail("edge " + i + ": expected " + refEdges[i] + "; got " + vEdges[i]);
    }

}

