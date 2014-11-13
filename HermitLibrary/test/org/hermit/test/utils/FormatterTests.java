
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

package org.hermit.test.utils;


import junit.framework.TestCase;

import org.hermit.utils.CharFormatter;


/**
 * Test geodetic calculations.
 *
 * @author	Ian Cameron Smith
 */
public class FormatterTests
    extends TestCase
{

    // ******************************************************************** //
    // Test Framework.
    // ******************************************************************** //

    @Override
    protected void setUp() {
        buf = new char[40];
    }

    private void run(int off, String val, int field, String expect) {
        String res = null;
        try {
            CharFormatter.formatString(buf, off, val, field);
            res = new String(buf, off, field);
        } catch (ArrayIndexOutOfBoundsException e) {
            res = "!OOB!";
        } catch (IllegalArgumentException e) {
            res = "!ILL!";
        }
        assertEquals(expect, res);
    }

    private void run(int off, String val, int field, boolean right, String expect) {
        String res = null;
        try {
            CharFormatter.formatString(buf, off, val, field, right);
            res = new String(buf, off, field);
        } catch (ArrayIndexOutOfBoundsException e) {
            res = "!OOB!";
        } catch (IllegalArgumentException e) {
            res = "!ILL!";
        }
        assertEquals(expect, res);
    }

    private void run(int off, int val, int field, boolean signed, String expect) {
        String res = null;
        try {
            CharFormatter.formatInt(buf, off, val, field, signed);
            res = new String(buf, off, field);
        } catch (ArrayIndexOutOfBoundsException e) {
            res = "!OOB!";
        } catch (IllegalArgumentException e) {
            res = "!ILL!";
        }
        assertEquals(expect, res);
    }

    private void runZ(int off, int val, int field, boolean signed, String expect) {
        String res = null;
        try {
            CharFormatter.formatInt(buf, off, val, field, signed, true);
            res = new String(buf, off, field);
        } catch (ArrayIndexOutOfBoundsException e) {
            res = "!OOB!";
        } catch (IllegalArgumentException e) {
            res = "!ILL!";
        }
        assertEquals(expect, res);
    }

    private void runL(int off, int val, int field, boolean signed, String expect) {
        String res = null;
        try {
            CharFormatter.formatIntLeft(buf, off, val, field, signed);
            res = new String(buf, off, field);
        } catch (ArrayIndexOutOfBoundsException e) {
            res = "!OOB!";
        } catch (IllegalArgumentException e) {
            res = "!ILL!";
        }
        assertEquals(expect, res);
    }

    private void run(int off, double val, int field, int frac, boolean signed, String expect) {
        String res = null;
        try {
            CharFormatter.formatFloat(buf, off, val, field, frac, signed);
            res = new String(buf, off, field);
        } catch (ArrayIndexOutOfBoundsException e) {
            res = "!OOB!";
        } catch (IllegalArgumentException e) {
            res = "!ILL!";
        }
        assertEquals(expect, res);
    }

    
    // ******************************************************************** //
    // String Tests.
    // ******************************************************************** //

    public void testStringLeftFix() {
        run(13, null,    7, "       ");
        run(13, "",      7, "       ");
        run(13, "ABCDE", 7, "ABCDE  ");
        run(13, "ABCDE", 7, false, "ABCDE  ");
        run(13, "ABCDE", 3, "ABC");
        run(13, "ABCDE", 3, false, "ABC");
    }
    

    public void testStringRightFix() {
        run(13, null,    7, true, "       ");
        run(13, "",      7, true, "       ");
        run(13, "ABCDE", 7, true, "  ABCDE");
        run(13, "ABCDE", 3, true, "CDE");
    }
    

    // ******************************************************************** //
    // Right-Aligned Integer Tests.
    // ******************************************************************** //

    public void testPosIntUns() {
        run(13, 173, 7, false, "    173");
        run(13, 173, 3, false, "173");
        run(13, 173, 2, false, " +");
        run(35, 173, 7, false, "!OOB!");
    }


    public void testZeroIntUns() {
        run(13, 0, 7, false, "      0");
        run(13, 0, 1, false, "0");
    }


    public void testNegIntUns() {
        run(13, -173, 7, false, "      -");
        run(13, -173, 3, false, "  -");
    }


    public void testPosIntSgn() {
        run(13, 173, 7, true, "    173");
        run(13, 173, 4, true, " 173");
        run(13, 173, 3, true, "  +");
        run(13, 173, 2, true, " +");
        run(35, 173, 7, true, "!OOB!");
    }


    public void testZeroIntSgn() {
        run(13, 0, 7, true, "      0");
        run(13, 0, 2, true, " 0");
        run(13, 0, 1, true, "!ILL!");
    }


    public void testNegIntSgn() {
        run(13, -173, 7, true, "   -173");
        run(13, -173, 4, true, "-173");
        run(13, -173, 3, true, "  +");
        run(13, -173, 2, true, " +");
    }


    // ******************************************************************** //
    // Right-Aligned Zero-Padded Integer Tests.
    // ******************************************************************** //

    public void testZPosIntUns() {
        runZ(13, 173, 7, false, "0000173");
        runZ(13, 173, 3, false, "173");
        runZ(13, 173, 2, false, " +");
        runZ(35, 173, 7, false, "!OOB!");
    }


    public void testZZeroIntUns() {
        runZ(13, 0, 7, false, "0000000");
        runZ(13, 0, 1, false, "0");
    }


    public void testZNegIntUns() {
        runZ(13, -173, 7, false, "      -");
        runZ(13, -173, 3, false, "  -");
    }


    public void testZPosIntSgn() {
        runZ(13, 173, 7, true, " 000173");
        runZ(13, 173, 4, true, " 173");
        runZ(13, 173, 3, true, "  +");
        runZ(13, 173, 2, true, " +");
        runZ(35, 173, 7, true, "!OOB!");
    }


    public void testZZeroIntSgn() {
        runZ(13, 0, 7, true, " 000000");
        runZ(13, 0, 2, true, " 0");
        runZ(13, 0, 1, true, "!ILL!");
    }


    public void testZNegIntSgn() {
        runZ(13, -173, 7, true, "-000173");
        runZ(13, -173, 4, true, "-173");
        runZ(13, -173, 3, true, "  +");
        runZ(13, -173, 2, true, " +");
    }


    // ******************************************************************** //
    // Left-Aligned Integer Tests.
    // ******************************************************************** //

    public void testLPosIntUns() {
        runL(13, 173, 7, false, "173    ");
        runL(13, 173, 3, false, "173");
        runL(13, 173, 2, false, "+ ");
        runL(35, 173, 7, false, "!OOB!");
    }


    public void testLZeroIntUns() {
        runL(13, 0, 7, false, "0      ");
        runL(13, 0, 1, false, "0");
    }


    public void testLNegIntUns() {
        runL(13, -173, 7, false, "-      ");
        runL(13, -173, 3, false, "-  ");
    }


    public void testLPosIntSgn() {
        runL(13, 173, 7, true, " 173   ");
        runL(13, 173, 4, true, " 173");
        runL(13, 173, 3, true, "+  ");
        runL(13, 173, 2, true, "+ ");
        runL(35, 173, 7, true, "!OOB!");
    }


    public void testLZeroIntSgn() {
        runL(13, 0, 7, true, " 0     ");
        runL(13, 0, 2, true, " 0");
        runL(13, 0, 1, true, "!ILL!");
    }


    public void testLNegIntSgn() {
        runL(13, -173, 7, true, "-173   ");
        runL(13, -173, 4, true, "-173");
        runL(13, -173, 3, true, "+  ");
        runL(13, -173, 2, true, "+ ");
    }


    // ******************************************************************** //
    // Float Tests.
    // ******************************************************************** //

    public void testPosFloatUns() {
        run(13, 173.45678, 7, 2, false, " 173.45");
        run(13, 173.45678, 7, 3, false, "173.456");
        run(13, 73.00678, 7, 4, false, "73.0067");
        run(13, 173.45678, 7, 4, false, "      +");
    }


    public void testZeroFloatUns() {
        run(13, 0, 7, 2, false, "   0.00");
        run(13, 0, 7, 3, false, "  0.000");
    }


    public void testNegFloatUns() {
        run(13, -173.45678, 7, 2, false, "      -");
        run(13, -173.45678, 7, 3, false, "      -");
        run(13, -73.00678, 7, 4, false, "      -");
    }


    public void testPosFloatSgn() {
        run(13, 173.45678, 7, 2, true, " 173.45");
        run(13, 73.00678, 7, 3, true, " 73.006");
        run(13, 173.45678, 7, 3, true, "      +");
        run(13, 73.00678, 7, 4, true, "      +");
    }


    public void testZeroFloatSgn() {
        run(13, 0, 7, 2, true, "   0.00");
        run(13, 0, 7, 3, true, "  0.000");
    }


    public void testNegFloatSgn() {
        run(13, -173.45678, 7, 2, true, "-173.45");
        run(13, -173.45678, 7, 2, true, "-173.45");
        run(13, -73.00678, 7, 3, true, "-73.006");
        run(13, -173.45678, 7, 3, true, "      +");
        run(13, -73.00678, 7, 4, true, "      +");
    }


    // ******************************************************************** //
    // Speed Tests.
    // ******************************************************************** //

    public void testSpeed() {
        final int COUNT = 100000;
        
        long j1 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; ++i)
            String.format("%7.2f", -(i / 1345678f));
        long j2 = System.currentTimeMillis();

        long c1 = System.currentTimeMillis();
        for (int i = 0; i < COUNT; ++i)
            CharFormatter.formatFloat(buf, 13, -(i / 1345678f), 7, 2, true);
        long c2 = System.currentTimeMillis();

        // CharFormatter should be at least 10 times faster.
        assertTrue((c2 - c1) * 10 < (j2 - j1));
    }


    // ******************************************************************** //
    // Member Data.
    // ******************************************************************** //

    private char[] buf;

}

