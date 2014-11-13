
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

package org.hermit.test.dsp;

import org.hermit.dsp.FFTTransformer;
import org.hermit.dsp.SignalPower;


/**
 * Test the signal power calculations.
 */
public class PowerTest {

    // ******************************************************************** //
    // Signal generation.
    // ******************************************************************** //

    private static short[] makeSine(double max, int rate, float freq, int buflen) {
        // Make a buffer of the right length.
        short[] buf = new short[buflen];
        
        // The length of 1 cycle in samples.
        int c = (int) (rate / freq);
        
        // Fill it with a sine wave.
        for (int i = 0; i < buflen; ++i) {
            long val = Math.round(Math.sin((double) i / c * Math.PI * 2) * max);
            if (val < Short.MIN_VALUE)
                buf[i] = Short.MIN_VALUE;
            else if (val > Short.MAX_VALUE)
                buf[i] = Short.MAX_VALUE;
            else
                buf[i] = (short) val;
        }
        
        return buf;
    }
    
    
    // ******************************************************************** //
    // Power Meter Testing.
    // ******************************************************************** //
    
    private static void runPowerTest(String name, double max, int rate, float freq, int buflen) {
        // Test on a buffer of all zeroes.
        short[] buf = makeSine(max, rate, freq, buflen);
        double power = SignalPower.calculatePowerDb(buf, 0, buf.length);
        System.out.format("%-8s@ %5dHz %5d/s: %10.5f\n", name, (int) freq, rate, power);
    }
    
    
    private static void runPowerAll(int rate, float freq) {
        int buflen = (int) (rate * 1f);

        // Test on a buffer of all zeroes.
        runPowerTest("Zero", 0f, rate, freq, buflen);
        
        // A truly miniscule signal; every 40th sample is 1, all others
        // are zero.
        runPowerTest("Tiny", 0.5f, rate, freq, buflen);
        
        // A very small signal; 5 1s, 15 0s, 5 -1s, 15 0s.
        runPowerTest("Small", 0.55f, rate, freq, buflen);
        
        // Minimum "real" signal, oscillating between 1 and -1.
        runPowerTest("Min", 1, rate, freq, buflen);
        
        // A full-range sine wave, from -32768 to 32767.
        runPowerTest("Full", 32768, rate, freq, buflen);
        
        // Maximum saturated signal.
        runPowerTest("Sat", 10000000, rate, freq, buflen);
        
        // Maximum saturated signal at a low frequency to reduce the small
        // values.  This is an unrealistically over-saturated signal.
        runPowerTest("Oversat", 10000000, rate, 80f, buflen);
    }
    

    // ******************************************************************** //
    // Spectrum Analyser Testing.
    // ******************************************************************** //
    
    private static void runFftTest(String name, double max, int rate, float freq,
                                   int buflen, FFTTransformer fft, float[] out)
    {
        // Test on a buffer of all zeroes.
        short[] buf = makeSine(max, rate, freq, buflen);
        fft.setInput(buf, 0, out.length * 2);
        fft.transform();
        fft.getResults(out);
        
        float minv = Float.MAX_VALUE;
        float maxv = Float.MIN_VALUE;
        for (int i = 0; i < out.length; ++i) {
            if (out[i] < minv)
                minv = out[i];
            if (out[i] > maxv)
                maxv = out[i];
        }
        
        System.out.format("%-8s@ %5dHz %5d/s: MIN %10.5f  MAX %10.5f\n", name, (int) freq, rate, minv, maxv);
    }


    private static void runFftAll(int rate, float freq, int fftBlock) {
        int buflen = (int) (rate * 1f);
        FFTTransformer fft = new FFTTransformer(fftBlock);
        float[] out = new float[fftBlock / 2];

        // Test on a buffer of all zeroes.
        runFftTest("Zero", 0f, rate, freq, buflen, fft, out);

        // A truly miniscule signal; every 40th sample is 1, all others
        // are zero.
        runFftTest("Tiny", 0.5f, rate, freq, buflen, fft, out);

        // A very small signal; 5 1s, 15 0s, 5 -1s, 15 0s.
        runFftTest("Small", 0.55f, rate, freq, buflen, fft, out);

        // Minimum "real" signal, oscillating between 1 and -1.
        runFftTest("Min", 1, rate, freq, buflen, fft, out);

        // A full-range sine wave, from -32768 to 32767.
        runFftTest("Full", 32768, rate, freq, buflen, fft, out);

        // Maximum saturated signal.
        runFftTest("Sat", 10000000, rate, freq, buflen, fft, out);
    }
    

    // ******************************************************************** //
    // Main.
    // ******************************************************************** //
    
	/**
	 * @param args
	 */
	public static void main(String[] args) {
	    // Run the tests at a couple of different sample rates.
	    runPowerAll(8000, 1000);
        runPowerAll(16000, 1000);
        
        runFftAll(8000, 250, 512);
        runFftAll(16000, 250, 512);
        runFftAll(16000, 500, 512);
        runFftAll(16000, 1000, 512);
        runFftAll(16000, 2000, 512);
	}
	
	
    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //
	
}

