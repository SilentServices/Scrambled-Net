
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

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;


/**
 * A graphical display which displays the audio waveform from an
 * {@link AudioAnalyser} instrument.  This class cannot be instantiated
 * directly; get an instance by calling
 * {@link AudioAnalyser#getWaveformGauge(SurfaceRunner)}.
 */
public class WaveformGauge
    extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a WaveformGauge.  This constructor is package-local, as
	 * public users get these from an {@link AudioAnalyser} instrument.
	 * 
	 * @param	parent			Parent surface.
	 */
	WaveformGauge(SurfaceRunner parent) {
	    super(parent);
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
	    
	    dispX = bounds.left;
	    dispY = bounds.top;
	    dispWidth = bounds.width();
	    dispHeight = bounds.height();

        // Create the bitmap for the audio waveform display,
        // and the Canvas for drawing into it.
        waveBitmap = getSurface().getBitmap(dispWidth, dispHeight);
        waveCanvas = new Canvas(waveBitmap);
	}

	
    // ******************************************************************** //
    // Data Updates.
    // ******************************************************************** //
    
	/**
	 * New data from the instrument has arrived.  This method is called
	 * on the thread of the instrument.
	 * 
     * @param   buffer      Audio data that was just read.
     * @param   off         Offset in data of the input data.
     * @param   len         Length of the input data.
     * @param   bias        Bias of the signal -- i.e. the offset of the
     *                      average signal value from zero.
     * @param   range       The range of the signal -- i.e. the absolute
     *                      value of the largest departure from the bias level.
	 */
	final void update(short[] buffer, int off, int len, float bias, float range) {
        final Canvas canvas = waveCanvas;
        final Paint paint = getPaint();
        
        // Calculate a scaling factor.  We want a degree of AGC, but not
        // so much that the waveform is always the same height.  Note we have
        // to take bias into account, otherwise we could scale the signal
        // off the screen.
        float scale = (float) Math.pow(1f / (range / 6500f), 0.7) / 16384 * dispHeight;
        if (scale < 0.001f || Float.isInfinite(scale))
            scale = 0.001f;
        else if (scale > 1000f)
            scale = 1000f;
        final float margin = dispWidth / 24;
        final float gwidth = dispWidth - margin * 2;
        final float baseY = dispHeight / 2f;
        final float uw = gwidth / (float) len;

        // Now actually do the drawing.
        synchronized (this) {
            canvas.drawColor(0xff000000);

            // Draw the axes.
            paint.setColor(0xffffff00);
            paint.setStyle(Style.STROKE);
            canvas.drawLine(margin, 0, margin, dispHeight - 1, paint);

            // Draw the waveform.  Drawing vertical lines up/down to the
            // waveform creates a "filled" effect, and is *much* faster
            // than drawing the waveform itself with diagonal lines.
            paint.setColor(0xffffff00);
            paint.setStyle(Style.STROKE);
            for (int i = 0; i < len; ++i) {
                final float x = margin + i * uw;
                final float y = baseY - (buffer[off + i] - bias) * scale;
                canvas.drawLine(x, baseY, x, y, paint);
            }
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
	    // Since drawBody may be called more often than we get audio
	    // data, it makes sense to just draw the buffered image here.
	    synchronized (this) {
	        canvas.drawBitmap(waveBitmap, dispX, dispY, null);
	    }
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
    
	// Display position and size within the parent view.
    private int dispX = 0;
    private int dispY = 0;
	private int dispWidth = 0;
	private int dispHeight = 0;
	
    // Bitmap in which we draw the audio waveform display,
    // and the Canvas and Paint for drawing into it.
    private Bitmap waveBitmap = null;
    private Canvas waveCanvas = null;

}

