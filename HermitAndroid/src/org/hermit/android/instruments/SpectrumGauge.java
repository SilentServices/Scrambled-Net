
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
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Paint.Style;


/**
 * A graphical display which displays the audio spectrum from an
 * {@link AudioAnalyser} instrument.  This class cannot be instantiated
 * directly; get an instance by calling
 * {@link AudioAnalyser#getSpectrumGauge(SurfaceRunner)}.
 */
public class SpectrumGauge
    extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a SpectrumGauge.  This constructor is package-local, as
	 * public users get these from an {@link AudioAnalyser} instrument.
	 * 
	 * @param	parent		Parent surface.
     * @param   rate        The input sample rate, in samples/sec.
	 */
	SpectrumGauge(SurfaceRunner parent, int rate) {
	    super(parent);
	    nyquistFreq = rate / 2;
	}


    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Set the sample rate for this instrument.
     * 
     * @param   rate        The desired rate, in samples/sec.
     */
    public void setSampleRate(int rate) {
        nyquistFreq = rate / 2;
        
        // If we have a size, then we have a background.  Re-draw it
        // to show the new frequency scale.
        if (haveBounds())
            drawBg(bgCanvas, getPaint());
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
	    
	    dispX = bounds.left;
	    dispY = bounds.top;
	    dispWidth = bounds.width();
	    dispHeight = bounds.height();
        
        // Do some layout within the meter.
        int mw = dispWidth;
        int mh = dispHeight;
        if (labelSize == 0f)
            labelSize = mw / 24f;
        
        spectLabY = mh - 4;
        spectGraphMargin = labelSize;
        spectGraphX = spectGraphMargin;
        spectGraphY = 0;
        spectGraphWidth = mw - spectGraphMargin * 2;
        spectGraphHeight = mh - labelSize - 6;

        // Create the bitmap for the spectrum display,
        // and the Canvas for drawing into it.
        specBitmap = getSurface().getBitmap(dispWidth, dispHeight);
        specCanvas = new Canvas(specBitmap);
        
        // Create the bitmap for the background,
        // and the Canvas for drawing into it.
        bgBitmap = getSurface().getBitmap(dispWidth, dispHeight);
        bgCanvas = new Canvas(bgBitmap);
        
        drawBg(bgCanvas, getPaint());
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
    private void drawBg(Canvas canvas, Paint paint) {
        canvas.drawColor(0xff000000);
        
        paint.setColor(0xffffff00);
        paint.setStyle(Style.STROKE);

        // Draw the grid.
        final float sx = 0 + spectGraphX;
        final float sy = 0 + spectGraphY;
        final float ex = sx + spectGraphWidth - 1;
        final float ey = sy + spectGraphHeight - 1;
        final float bw = spectGraphWidth - 1;
        final float bh = spectGraphHeight - 1;
        canvas.drawRect(sx, sy, ex, ey, paint);
        for (int i = 1; i < 10; ++i) {
            final float x = (float) i * (float) bw / 10f;
            canvas.drawLine(sx + x, sy, sx + x, sy + bh, paint);
        }
        for (int i = 1; i < RANGE_BELS; ++i) {
            final float y = (float) i * (float) bh / RANGE_BELS;
            canvas.drawLine(sx, sy + y, sx + bw, sy + y, paint);
        }
        
        // Draw the labels below the grid.
        final float ly = 0 + spectLabY;
        paint.setTextSize(labelSize);
        int step = paint.measureText("8.8k") > bw / 10f - 1 ? 2 : 1;
        for (int i = 0; i <= 10; i += step) {
            int f = nyquistFreq * i / 10;
            String text = f >= 10000 ? "" + (f / 1000) + "k" :
                          f >= 1000 ? "" + (f / 1000) + "." + (f / 100 % 10) + "k" :
                          "" + f;
            float tw = paint.measureText(text);
            float lx = sx + i * (float) bw / 10f + 1 - (tw / 2);
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
     * @param   data        An array of floats defining the signal power
     *                      at each frequency in the spectrum.
	 */
	final void update(float[] data) {
        final Canvas canvas = specCanvas;
        final Paint paint = getPaint();
        
        // Now actually do the drawing.
        synchronized (this) {
            canvas.drawBitmap(bgBitmap, 0, 0, paint);
            if (logFreqScale)
                logGraph(data, canvas, paint);
            else
                linearGraph(data, canvas, paint);
        }
    }

	   
    /**
     * Draw a linear spectrum graph.
     * 
     * @param   data        An array of floats defining the signal power
     *                      at each frequency in the spectrum.
     * @param  canvas       Canvas to draw into.
     * @param  paint        Paint to draw with.
     */
    private void logGraph(float[] data, Canvas canvas, Paint paint) {
        paint.setStyle(Style.FILL);
        paintColor[1] = 1f;
        paintColor[2] = 1f;
        final int len = data.length;
        final float bw = (float) (spectGraphWidth - 2) / (float) len;
        final float bh = spectGraphHeight - 2;
        final float be = spectGraphY + spectGraphHeight - 1;
        
        // Determine the first and last frequencies we have.
        final float lf = nyquistFreq / len;
        final float rf = nyquistFreq;
        
        // Now, how many octaves is that.  Round down.  Calculate pixels/oct.
        final int octaves = (int) Math.floor(log2(rf / lf)) - 2;
        final float octWidth = (float) (spectGraphWidth - 2) / (float) octaves;
        
        // Calculate the base frequency for the graph, which isn't lf.
        final float bf = rf / (float) Math.pow(2, octaves);
            
        // Element 0 isn't a frequency bucket; skip it.
        for (int i = 1; i < len; ++i) {
            // Cycle the hue angle from 0° to 300°; i.e. red to purple.
            paintColor[0] = (float) i / (float) len * 300f;
            paint.setColor(Color.HSVToColor(paintColor));

            // What frequency bucket are we in.
            final float f = lf * i;

            // For freq f, calculate x.
            final float x = spectGraphX + (float) (log2(f) - log2(bf)) * octWidth;

            // Draw the bar.
            float y = be - (float) (Math.log10(data[i]) / RANGE_BELS + 1f) * bh;
            if (y > be)
                y = be;
            else if (y < spectGraphY)
                y = spectGraphY;
            if (bw <= 1.0f)
                canvas.drawLine(x, y, x, be, paint);
            else
                canvas.drawRect(x, y, x + bw, be, paint);
        }
    }
    
    
    private final double log2(double x) {
        return Math.log(x) / LOG2;
    }
    

	/**
	 * Draw a linear spectrum graph.
	 * 
     * @param   data        An array of floats defining the signal power
     *                      at each frequency in the spectrum.
	 * @param  canvas       Canvas to draw into.
	 * @param  paint        Paint to draw with.
	 */
	private void linearGraph(float[] data, Canvas canvas, Paint paint) {
        paint.setStyle(Style.FILL);
        paintColor[1] = 1f;
        paintColor[2] = 1f;
        final int len = data.length;
        final float bw = (float) (spectGraphWidth - 2) / (float) len;
        final float bh = spectGraphHeight - 2;
        final float be = spectGraphY + spectGraphHeight - 1;
        
        // Element 0 isn't a frequency bucket; skip it.
        for (int i = 1; i < len; ++i) {
            // Cycle the hue angle from 0° to 300°; i.e. red to purple.
            paintColor[0] = (float) i / (float) len * 300f;
            paint.setColor(Color.HSVToColor(paintColor));

            // Draw the bar.
            final float x = spectGraphX + i * bw + 1;
            float y = be - (float) (Math.log10(data[i]) / RANGE_BELS + 1f) * bh;
            if (y > be)
                y = be;
            else if (y < spectGraphY)
                y = spectGraphY;
            if (bw <= 1.0f)
                canvas.drawLine(x, y, x, be, paint);
            else
                canvas.drawRect(x, y, x + bw, be, paint);
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
	        canvas.drawBitmap(specBitmap, dispX, dispY, null);
	    }
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";
	
	// Log of 2.
	private static final double LOG2 = Math.log(2);

    // Vertical range of the graph in bels.
    private static final float RANGE_BELS = 6f;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
    // The Nyquist frequency -- the highest frequency
    // represented in the spectrum data we will be plotting.
    private int nyquistFreq = 0;

    // If true, draw a logarithmic frequency scale.  Otherwise linear.
    private static final boolean logFreqScale = false;

	// Display position and size within the parent view.
    private int dispX = 0;
    private int dispY = 0;
	private int dispWidth = 0;
	private int dispHeight = 0;
    
    // Label text size for the gauge.  Zero means not set yet.
    private float labelSize = 0f;

    // Layout parameters for the VU meter.  Position and size for the
    // bar itself; position and size for the bar labels; position
    // and size for the main readout text.
    private float spectGraphX = 0;
    private float spectGraphY = 0;
    private float spectGraphWidth = 0;
    private float spectGraphHeight = 0;
    private float spectLabY = 0;
    private float spectGraphMargin = 0;

    // Bitmap in which we draw the gauge background,
    // and the Canvas and Paint for drawing into it.
    private Bitmap bgBitmap = null;
    private Canvas bgCanvas = null;

    // Bitmap in which we draw the audio spectrum display,
    // and the Canvas and Paint for drawing into it.
    private Bitmap specBitmap = null;
    private Canvas specCanvas = null;

    // Buffer for calculating the draw colour from H,S,V values.
    private float[] paintColor = { 0, 1, 1 };

}

