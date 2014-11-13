
/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hermit.test;


import static java.lang.Math.abs;
import junit.framework.Assert;


/**
 * Contains additional assertion methods not found in JUnit.
 */
public final class NumericAsserts {

    private NumericAsserts() { }


    /**
     * Asserts that {@code value} is in the range {@code min .. @code max}
     * inclusive.
     */
    public static void assertRange(String message, double value, double min, double max) {
        if (value < min || value > max)
            failWithMessage(message, "expected to be in range [" +
            						 min + "," + max + "] but was " + value);
    }


    /**
     * Asserts that {@code value} is in the range
     * {@code expect-maxerr .. @code expect+maxerr} inclusive.
     */
    public static void assertTolerance(String message, double value, double expect, double maxerr) {
    	double err = abs(value - expect);
        if (err > maxerr)
            failWithMessage(message, "expected to be within " +
            						 maxerr + " of " + expect +
            						 " but was " + value);
    }


    /**
     * Asserts that {@code value} is in the range
     * {@code expect-maxerr .. @code expect+maxerr} inclusive, where maxerr is
     * {@code expect*pcnt/100}.
     */
    public static void assertPercent(String message, double value, double expect, double pcnt) {
    	double errpcnt = abs(value - expect) / expect * 100.0;
        if (errpcnt > pcnt)
            failWithMessage(message, "expected to be within " +
            						 pcnt + "% of " + expect +
            						 " but was " + value);
    }


    /**
     * Asserts that a given angle {@code angle} is equal to {@code expect},
     * plus or minus {@code maxerr} degrees.  Takes care of angles going
     * past 360 / 0.
     */
    public static void assertDegrees(String message, double value, double expect, double maxerr) {
    	if (maxerr < 180.0 && value <= maxerr && expect >= 360.0 - maxerr)
    		value += 360.0;
    	else if (maxerr < 180.0 && expect <= maxerr && value >= 360.0 - maxerr)
    		expect += 360.0;
    	double err = abs(value - expect);
        if (err > maxerr)
            failWithMessage(message, "expected to be within " +
            						 maxerr + " of " + expect +
            						 " but was " + value);
    }


    private static void failWithMessage(String userMessage, String ourMessage) {
        Assert.fail(userMessage == null ? ourMessage : userMessage + ' ' + ourMessage);
    }

}

