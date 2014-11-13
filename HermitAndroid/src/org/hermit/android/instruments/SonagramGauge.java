
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
 * A graphical display which displays the audio sonarum from an
 * {@link AudioAnalyser} instrument.  This class cannot be instantiated
 * directly; get an instance by calling
 * {@link AudioAnalyser#getSpectrumGauge(SurfaceRunner)}.
 */
public class SonagramGauge
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
	SonagramGauge(SurfaceRunner parent, int rate,int inputBlockSize) {
	    super(parent);
	    blockSize=inputBlockSize;
	    setSampleRate(rate);

	    //Colors for Sonagram
	    for (int i=0;i<50;i++) 
	    	paintColors[i]= Color.rgb(0, i , i*5);
	    for (int i=50;i<100;i++) 
	    	paintColors[i]= Color.rgb(0, i , (100-i)*5);
	    for (int i=100;i<150;i++) 
	    	paintColors[i]= Color.rgb((i-100)*3, (i-50)*2 , 0);
	    for (int i=150;i<=250;i++) 
	    	paintColors[i]= Color.rgb(i, 550-i*2 , 0);
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
    	period=(float)(1f / rate)*blockSize*2;
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
        
        sonaGraphX = 0;
        sonaGraphY = 3;        
        sonaGraphWidth = mw - labelSize * 2;
        sonaGraphHeight = mh - labelSize - 6 ;

        // Create the bitmap for the sonagram display,
        // and the Canvas for drawing into it.
        finalBitmap = getSurface().getBitmap(dispWidth, dispHeight);
        finalCanvas = new Canvas(finalBitmap);

        // Create the bitmap for the sonagram display,
        // and the Canvas for drawing into it.
        sonaBitmap = getSurface().getBitmap((int) sonaGraphWidth, (int) sonaGraphHeight);
        sonaCanvas = new Canvas(sonaBitmap);
        
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
     * Do the subclass-sonaific parts of drawing the background
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
        float lx;
        float ly;
        canvas.drawColor(0xff000000);
        
        paint.setColor(0xffffff00);
        paint.setStyle(Style.STROKE);

        // Draw the grid.
        final float sx = sonaGraphX;
        final float sy = sonaGraphY;
        final float bw = sonaGraphWidth - 1;
        final float bh = sonaGraphHeight - 1;
        
        // Draw freq.        
        lx = sx + bw + 1;
        for (int i = 0; i <= 10; i += 1) {
            int f = nyquistFreq * i / 10;
            String text = f >= 10000 ? "" + (f / 1000) + "k" :
                          f >= 1000 ? "" + (f / 1000) + "." + (f / 100 % 10) + "k" :
                          "" + f;
            ly = sy + bh - i * (float) bh / 10f + 1;
            canvas.drawText(text, lx + 7, ly + labelSize/3, paint);
            canvas.drawLine(lx, ly, lx+3, ly, paint);
        }

        // Draw time.        
        ly = sy + bh + labelSize + 1;
        float totaltime=(float)Math.floor(bw*period);
        for (int i = 0; i <= 9; i += 1) {
            float time = totaltime * i / 10;
            String text = "" + time + "s";
            float tw = paint.measureText(text);
            lx = sx + i * (float) bw / 10f + 1;
            canvas.drawText(text, lx - (tw / 2), ly, paint);
            canvas.drawLine(lx, sy + bh-1, lx, sy + bh + 2, paint);
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
     *                      at each frequency in the sonagram.
	 */
	final void update(float[] data) {
        final Canvas canvas = finalCanvas;
        final Paint paint = getPaint();
        
        // Now actually do the drawing.
        synchronized (this) {
            //Background
        	canvas.drawBitmap(bgBitmap, 0, 0, paint);
            
        	//Scroll
            sonaCanvas.drawBitmap(sonaBitmap, 1, 0, paint);       
            
            //Add Current Data
            linearGraph(data, sonaCanvas, paint);                   
            canvas.drawBitmap(sonaBitmap, sonaGraphX, sonaGraphY, paint);
        }
    }
   
	/**
	 * Draw a linear sonagram graph.
	 * 
     * @param   data        An array of floats defining the signal power
     *                      at each frequency in the sonagram.
	 * @param  canvas       Canvas to draw into.
	 * @param  paint        Paint to draw with.
	 */
	private void linearGraph(float[] data, Canvas canvas, Paint paint) {
        paint.setStyle(Style.FILL);
        final int len = data.length;
        final float bh = (float) sonaGraphHeight / (float) len;
        
        // Element 0 isn't a frequency bucket; skip it.
        for (int i = 1; i < len; ++i) {
            // Draw the new line.
            final float y = sonaGraphHeight- i * bh + 1;

            // Cycle the hue angle from 0° to 300°; i.e. red to purple.
            float v = (float) (Math.log10(data[i]) / RANGE_BELS + 2f);
            int colorIndex=(int)(v*maxColors);
            if (colorIndex<0)
            	colorIndex=0;
            if (colorIndex>maxColors)
            	colorIndex=maxColors;
            paint.setColor(paintColors[colorIndex]);
            
            if (bh <= 1.0f)
                canvas.drawPoint(0, y, paint);
            else
                canvas.drawLine(0, y, 0, y - bh, paint);
        }
	}
	

	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * Do the subclass-sonaific parts of drawing for this element.
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
	        canvas.drawBitmap(finalBitmap, dispX, dispY, null);
	    }
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";
	
    // Vertical range of the graph in bels.
    private static final float RANGE_BELS = 2f;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
    // The Nyquist frequency -- the highest frequency
    // represented in the sonarum data we will be plotting.
    private int nyquistFreq = 0;
    private int blockSize = 0;
    private float period = 0;

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
    private float sonaGraphX = 0;
    private float sonaGraphY = 0;
    private float sonaGraphWidth = 0;
    private float sonaGraphHeight = 0;

    // Bitmap in which we draw the gauge background,
    // and the Canvas and Paint for drawing into it.
    private Bitmap bgBitmap = null;
    private Canvas bgCanvas = null;

    // Bitmap in which we draw the audio sonagram display,
    // and the Canvas and Paint for drawing into it.
    private Bitmap sonaBitmap = null;
    private Canvas sonaCanvas = null;

    // Bitmap in which we draw the audio sonagram display,
    // and the Canvas and Paint for drawing into it.
    private Bitmap finalBitmap = null;
    private Canvas finalCanvas = null;

    
    // Buffer for calculating the draw colour from H,S,V values.
    private final int[] paintColors= new int[251];
    private final int maxColors=250;

}

