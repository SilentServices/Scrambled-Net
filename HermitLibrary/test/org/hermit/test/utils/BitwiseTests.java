
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

import org.hermit.utils.Bitwise;


/**
 * Test bitwise utility functions.
 *
 * @author	Ian Cameron Smith
 */
public class BitwiseTests
    extends TestCase
{

    // ******************************************************************** //
    // Test Framework.
    // ******************************************************************** //

    private void runBitrev(int val, int n, int expect) {
        int res = Bitwise.bitrev(val, n);
        assertEquals("bitrev", String.format("0x%08x", expect), String.format("0x%08x", res));
    }

    
    // ******************************************************************** //
    // Tests.
    // ******************************************************************** //

    public void testSixteen() {
        runBitrev(0xfda91235,  2, 0x00000002);
        runBitrev(0xfda91235,  3, 0x00000005);
        runBitrev(0xfda91233,  3, 0x00000006);
        runBitrev(0xfda91235, 16, 0x0000ac48);
    }


    public void testCorners() {
        runBitrev(0xfda91234,  0, 0x00000000);
        runBitrev(0xfda91235,  1, 0x00000001);
        runBitrev(0xfda91235, 32, 0xac4895bf);
    }

}

