
/**
 * org.hermit.android.instrument: graphical instruments for Android.
 * <br>Copyright 2009 Ian Cameron Smith
 * 
 * <p>These classes provide input and display functions for creating on-screen
 * instruments of various kinds in Android apps.
 *
 * <p>This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2
 * as published by the Free Software Foundation (see COPYING).
 * 
 * <p>This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 */


package org.hermit.android.instruments;


import org.hermit.android.core.SurfaceRunner;
import org.hermit.utils.CharFormatter;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;


/**
 * A graphical display which displays the signal power in dB from an
 * {@link AudioAnalyser} instrument.  This class cannot be instantiated
 * directly; get an instance by calling
 * {@link AudioAnalyser#getPowerGauge(SurfaceRunner)}.
 */
public class PowerGauge
    extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a PowerGauge.  This constructor is package-local, as
	 * public users get these from an {@link AudioAnalyser} instrument.
	 * 
	 * @param	parent			Parent surface.
	 */
	PowerGauge(SurfaceRunner parent) {
	    super(parent);
        
        meterPeaks = new float[METER_PEAKS];
        meterPeakTimes = new long[METER_PEAKS];
        
        // Create and initialize the history buffer.
        powerHistory = new float[METER_AVERAGE_COUNT];
        for (int i = 0; i < METER_AVERAGE_COUNT; ++i)
            powerHistory[i] = -100.0f;
        averagePower = -100.0f;
        
        // Create the buffers where the text labels are formatted.
        dbBuffer = "-100.0dB".toCharArray();
        pkBuffer = "-100.0dB peak".toCharArray();
	}


	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

	/**
	 * Set the overall thickness of the bar.
	 * 
	 * @param  width       Overall width in pixels of the bar.
	 */
	public void setBarWidth(int width) {
	    barWidth = width;
	}
	

    /**
     * Set the size for the label text.
     * 
     * @param   size        Label text size for the gauge.
     */
    public void setLabelSize(float size) {
        labelSize = size;
    }


    /**
     * Get the size for the label text.
     * 
     * @return              Label text size for the gauge.
     */
    public float getLabelSize() {
        return labelSize;
    }


    // ******************************************************************** //
    // Geometry.
    // ******************************************************************** //

    /**
     * This is called during layout when the size of this element has
     * changed.  This is where we first discover our size, so set
     * our geometry to match.
     * 
	 * @param	bounds		The bounding rect of this element within
	 * 						its parent View.
     */
	@Override
    public void setGeometry(Rect bounds) {
        super.setGeometry(bounds);

        Paint paint = getPaint();
	    
	    dispX = bounds.left;
	    dispY = bounds.top;
	    dispWidth = bounds.width();
	    dispHeight = bounds.height();
        
        // Do some layout within the meter.
        int mw = dispWidth;
        int mh = dispHeight;
        if (labelSize == 0f)
            labelSize = mw / 24f;
       
        // The bar and labels.
        meterBarTop = 0;
        meterBarGap = barWidth / 4;
        meterLabY = meterBarTop + barWidth + labelSize;
        
        // Horizontal margins.
        meterBarMargin = labelSize;
        
        // Text readouts.  First try putting them side by side.
        float th = mh - meterLabY;
        float subWidth = (mw - meterBarMargin * 2) / 2;

        meterTextSize = findTextSize(subWidth, th, "-100.0dB", paint);
        paint.setTextSize(meterTextSize);
        meterTextX = meterBarMargin;
        meterTextY = mh - paint.descent();

        meterSubTextSize = findTextSize(subWidth, th, "-100.0dB peak", paint);
        paint.setTextSize(meterSubTextSize);
        meterSubTextX = meterTextX + subWidth;
        meterSubTextY = mh - paint.descent();

        // If we have tons of empty space, stack the text readouts vertically.
        if (meterTextSize <= th / 2) {
            float split = th * 1f / 3f;
            meterTextSize = (th - split) * 0.9f;
            paint.setTextSize(meterTextSize);
            float tw = paint.measureText("-100.0dB");
            meterTextX = (mw - tw) / 2f;
            meterTextY = mh - split - paint.descent();
            
            meterSubTextSize = (th - meterTextSize) * 0.9f;
            paint.setTextSize(meterSubTextSize);
            float pw = paint.measureText("-100.0dB peak");
            meterSubTextX = (mw - pw) / 2f;
            meterSubTextY = mh - paint.descent();
        }
        
        // Cache our background image.
        cacheBackground();
	}
	
	
	private float findTextSize(float w, float h, String template, Paint paint) {
        float size = h;
        do {
            paint.setTextSize(size);
            int sw = (int) paint.measureText(template);
            if (sw <= w)
                break;
            --size;
        } while (size > 12);
        
        return size;
	}


    // ******************************************************************** //
    // Background Drawing.
    // ******************************************************************** //
    
    /**
     * Do the subclass-specific parts of drawing the background
     * for this element.  Subclasses should override
     * this if they have significant background content which they would
     * like to draw once only.  Whatever is drawn here will be saved in
     * a bitmap, which will be rendered to the screen before the
     * dynamic content is drawn.
     * 
     * <p>Obviously, if implementing this method, don't clear the screen when
     * drawing the dynamic part.
     * 
     * @param   canvas      Canvas to draw into.
     * @param   paint       The Paint which was set up in initializePaint().
     */
    @Override
    protected void drawBackgroundBody(Canvas canvas, Paint paint) {
        canvas.drawColor(0xff000000);
        
        paint.setColor(0xffffff00);
        paint.setStyle(Style.STROKE);

        // Draw the grid.
        final float mx = dispX + meterBarMargin;
        final float mw = dispWidth - meterBarMargin * 2;
        final float by = dispY + meterBarTop;
        final float bw = mw - 1;
        final float bh = barWidth - 1;
        final float gw = bw / 10f;
        canvas.drawRect(mx, by, mx + bw, by + bh, paint);
        for (int i = 1; i < 10; ++i) {
            final float x = (float) i * (float) bw / 10f;
            canvas.drawLine(mx + x, by, mx + x, by + bh, paint);
        }

        // Draw the labels below the grid.
        final float ly = dispY + meterLabY;
        final float ls = labelSize;
        paint.setTextSize(ls);
        int step = paint.measureText("-99") > bw / 10f - 1 ? 2 : 1;
        for (int i = 0; i <= 10; i += step) {
            String text = "" + (i * 10 - 100);
            float tw = paint.measureText(text);
            float lx = mx + i * gw + 1 - (tw / 2);
            canvas.drawText(text, lx, ly, paint);
        }
    }


    // ******************************************************************** //
    // Data Updates.
    // ******************************************************************** //
    
	/**
	 * New data from the instrument has arrived.  This method is called
	 * on the thread of the instrument.
	 * 
     * @param   power       The current instantaneous signal power level
     *                      in dB, from -Inf to 0+.  Typical range is
     *                      -100dB to 0dB, 0dB representing max. input power.
	 */
    final void update(double power) {
        synchronized (this) {
            // Save the current level.  Clip it to a reasonable range.
            if (power < -100.0)
                power = -100.0;
            else if (power > 0.0)
                power = 0.0;
            currentPower = (float) power;

            // Get the previous power value, and add the new value into the
            // history buffer.  Re-calculate the rolling average power value.
            if (++historyIndex >= powerHistory.length)
                historyIndex = 0;
            prevPower = powerHistory[historyIndex];
            powerHistory[historyIndex] = (float) power;
            averagePower -= prevPower / METER_AVERAGE_COUNT;
            averagePower += (float) power / METER_AVERAGE_COUNT;
        }
    }

    
	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * Do the subclass-specific parts of drawing for this element.
	 * This method is called on the thread of the containing SuraceView.
	 * 
	 * <p>Subclasses should override this to do their drawing.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	@Override
    protected final void drawBody(Canvas canvas, Paint paint, long now) {
	    synchronized (this) {
	        // Re-calculate the peak markers.
	        calculatePeaks(now, currentPower, prevPower);

	        paint.setColor(0xffffff00);
	        paint.setStyle(Style.STROKE);

	        // Position parameters.
	        final float mx = dispX + meterBarMargin;
	        final float mw = dispWidth - meterBarMargin * 2;
	        final float by = dispY + meterBarTop;
	        final float bh = barWidth;
	        final float gap = meterBarGap;
	        final float bw = mw - 2f;
	        
	        // Draw the average bar.
	        final float pa = (averagePower / 100f + 1f) * bw;
	        paint.setStyle(Style.FILL);
	        paint.setColor(METER_AVERAGE_COL);
	        canvas.drawRect(mx + 1, by + 1, mx + pa + 1, by + bh - 1, paint);

	        // Draw the power bar.
	        final float p = (currentPower / 100f + 1f) * bw;
	        paint.setStyle(Style.FILL);
	        paint.setColor(METER_POWER_COL);
	        canvas.drawRect(mx + 1, by + gap, mx + p + 1, by + bh - gap, paint);

	        // Now, draw in the peaks.
	        paint.setStyle(Style.FILL);
	        for (int i = 0; i < METER_PEAKS; ++i) {
	            if (meterPeakTimes[i] != 0) {
	                // Fade the peak according to its age.
	                long age = now - meterPeakTimes[i];
	                float fac = 1f - ((float) age / (float) METER_PEAK_TIME);
	                int alpha = (int) (fac * 255f);
	                paint.setColor(METER_PEAK_COL | (alpha << 24));
	                // Draw it in.
	                final float pp = (meterPeaks[i] / 100f + 1f) * bw;
	                canvas.drawRect(mx + pp - 1, by + gap,
	                        mx + pp + 3, by + bh - gap, paint);
	            }
	        }

	        // Draw the text below the meter.
            final float tx = dispX + meterTextX;
	        final float ty = dispY + meterTextY;
	        CharFormatter.formatFloat(dbBuffer, 0, averagePower, 6, 1);
	        paint.setStyle(Style.STROKE);
	        paint.setColor(0xff00ffff);
	        paint.setTextSize(meterTextSize);
	        canvas.drawText(dbBuffer, 0, dbBuffer.length, tx, ty, paint);
	        
            final float px = dispX + meterSubTextX;
            final float py = dispY + meterSubTextY;
            CharFormatter.formatFloat(pkBuffer, 0, meterPeakMax, 6, 1);
            paint.setTextSize(meterSubTextSize);
            canvas.drawText(pkBuffer, 0, pkBuffer.length, px, py, paint);
	    }
	}
	

    /**
     * Re-calculate the positions of the peak markers in the VU meter.
     */
    private final void calculatePeaks(long now, float power, float prev) {
        // First, delete any that have been passed or have timed out.
        for (int i = 0; i < METER_PEAKS; ++i) {
            if (meterPeakTimes[i] != 0 &&
                    (meterPeaks[i] < power ||
                     now - meterPeakTimes[i] > METER_PEAK_TIME))
                meterPeakTimes[i] = 0;
        }
        
        // If the meter has gone up, set a new peak, if there's an empty
        // slot.  If there isn't, don't bother, because we would be kicking
        // out a higher peak, which we don't want.
        if (power > prev) {
            boolean done = false;
            
            // First, check for a slightly-higher existing peak.  If there
            // is one, just bump its time.
            for (int i = 0; i < METER_PEAKS; ++i) {
                if (meterPeakTimes[i] != 0 && meterPeaks[i] - power < 2.5) {
                    meterPeakTimes[i] = now;
                    done = true;
                    break;
                }
            }
            
            if (!done) {
                // Now scan for an empty slot.
                for (int i = 0; i < METER_PEAKS; ++i) {
                    if (meterPeakTimes[i] == 0) {
                        meterPeaks[i] = power;
                        meterPeakTimes[i] = now;
                        break;
                    }
                }
            }
        }
        
        // Find the highest peak value.
        meterPeakMax = -100f;
        for (int i = 0; i < METER_PEAKS; ++i)
            if (meterPeakTimes[i] != 0 && meterPeaks[i] > meterPeakMax)
                meterPeakMax = meterPeaks[i];
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";

    // Number of peaks we will track in the VU meter.
    private static final int METER_PEAKS = 4;

    // Time in ms over which peaks in the VU meter fade out.
    private static final int METER_PEAK_TIME = 4000;

    // Number of updates over which we average the VU meter to get
    // a rolling average.  32 is about 2 seconds.
    private static final int METER_AVERAGE_COUNT = 32;

    // Colours for the meter power bar and average bar and peak marks.
    // In METER_PEAK_COL, alpha is set dynamically in the code.
    private static final int METER_POWER_COL = 0xff0000ff;
    private static final int METER_AVERAGE_COL = 0xa0ff9000;
    private static final int METER_PEAK_COL = 0x00ff0000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
    // Configured meter bar thickness.
    private int barWidth = 32;
    
	// Display position and size within the parent view.
    private int dispX = 0;
    private int dispY = 0;
	private int dispWidth = 0;
	private int dispHeight = 0;
    
    // Label text size for the gauge.  Zero if not set yet.
    private float labelSize = 0f;

    // Layout parameters for the VU meter.  Position and size for the
    // bar itself; position and size for the bar labels; position
    // and size for the main readout text.
    private float meterBarTop = 0;
    private float meterBarGap = 0;
    private float meterLabY = 0;
    private float meterTextX = 0;
    private float meterTextY = 0;
    private float meterTextSize = 0;
    private float meterSubTextX = 0;
    private float meterSubTextY = 0;
    private float meterSubTextSize = 0;
    private float meterBarMargin = 0;

    // Current and previous power levels.
    private float currentPower = 0f;
    private float prevPower = 0f;
    
    // Buffered old meter levels, used to calculate the rolling average.
    // Index of the most recent value.
    private float[] powerHistory = null;
    private int historyIndex = 0;
    
    // Rolling average power value,  calculated from the history buffer.
    private float averagePower = -100.0f;
    
    // Peak markers in the VU meter, and the times for each one.  A zero
    // time indicates a peak not set.
    private float[] meterPeaks = null;
    private long[] meterPeakTimes = null;
    private float meterPeakMax = 0f;

    // Buffer for displayed average and peak dB value texts.
    private char[] dbBuffer = null;
    private char[] pkBuffer = null;

}

