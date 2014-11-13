
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


import java.util.ArrayList;

import org.hermit.android.core.SurfaceRunner;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;


/**
 * Common base for applications which display instruments.  This class is
 * an extension of {@link SurfaceRunner} which provides additional
 * functions to manage the instruments embedded in it.
 * 
 * <p>When using this class in an app, the app context <b>must</b> call
 * these methods (usually from its corresponding Activity methods):
 * 
 * <ul>
 * <li>{@link #onStart()}
 * <li>{@link #onResume()}
 * <li>{@link #onPause()}
 * <li>{@link #onStop()}
 * </ul>
 * 
 * <p>The surface is enabled once it is created and sized, and
 * {@link #onStart()} and {@link #onResume()} have been called.  You then
 * start and stop it by calling {@link #surfaceStart()} and
 * {@link #surfaceStop()}.
 */
public abstract class InstrumentSurface
	extends SurfaceRunner
{

    // ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Instrument Surface runner option: cache a background bitmap.  If set,
     * we will ask all the gauges to draw their backgrounds into a full-
     * screen bitmap; this bitmap will then be drawn prior to drawing the
     * gauge contents each frame.
     */
    public static final int SURFACE_CACHE_BG = 0x0100;
    

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a SurfaceRunner instance.
     * 
     * @param   app         The application context we're running in.
     */
    public InstrumentSurface(Activity app) {
        super(app);
        init();
    }
    

    /**
     * Create a SurfaceRunner instance.
     * 
     * @param   app         The application context we're running in.
     * @param   options     Options for this SurfaceRunner.  A bitwise OR of
     *                      SURFACE_XXX constants.
     */
    public InstrumentSurface(Activity app, int options) {
        super(app, options);
        init();
    }
    
    
    /**
     * Do initialisation of this class.
     */
    private void init() {
        instruments = new ArrayList<Instrument>();
        gauges = new ArrayList<Gauge>();
    }
    
    
    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Add an instrument to the system, associated with this surface.
     * Is is presumed that the app will create one or more {@link Gauge}s
     * for  this instrument and add them to the surface with
     * {@link #addGauge(Gauge)}.
     * 
     * <p>All instruments added here will be started and stopped when the
     * application starts and stops, and will have their measurement turned
     * on when the app is visible and running.  Their
     * {@link Instrument#doUpdate(long)} method will be called each
     * time round the main animation loop.
     * 
     * <p>All instruments must be added before the application starts running.
     * 
     * @param   i           The instrument to add.
     */
    public void addInstrument(Instrument i) {
        instruments.add(i);
    }
    

    /**
     * Add a gauge to this surface.  If this gauge has an associated
     * {@link Instrument}, it should be attached to the surface with
     * {@link #addInstrument(Instrument)}.
     * 
     * <p>All gauges added here will have their
     * {@link Gauge#draw(Canvas, long, boolean)} method called each
     * time round the main animation loop.
     * 
     * <p>All gauges must be added before the application starts running.
     * 
     * @param   i           The gauge to add.
     */
    public void addGauge(Gauge i) {
        gauges.add(i);
    }


    /**
     * Remove all gauges from this surface.
     */
    public void clearGauges() {
        gauges.clear();
    }
    
    
    // ******************************************************************** //
    // Layout Processing.
    // ******************************************************************** //

    /**
     * Lay out the display for a given screen size.  Subclasses must
     * implement this, and should use it to lay out the gauges.
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     */
    protected abstract void layout(int width, int height);
    

    // ******************************************************************** //
    // Run Control.
    // ******************************************************************** //

    /**
     * The application is starting.  Perform any initial set-up prior to
     * starting the application.  We may not have a screen size yet,
     * so this is not a good place to allocate resources which depend on
     * that.
     * 
     * <p>If subclasses override this, they must call through to this method.
     */
    @Override
    protected void appStart() {
        for (Instrument i : instruments)
            i.appStart();
    }
    

    /**
     * Set the screen size.  This is guaranteed to be called before
     * animStart(), but perhaps not before appStart().
     * 
     * <p>We call the layout() method here, so subclasses generally don't
     * need to override this.  If subclasses do override this,
     * they must call through to this method.
     * 
     * @param   width       The new width of the surface.
     * @param   height      The new height of the surface.
     * @param   config      The pixel format of the surface.
     */
    @Override
    protected void appSize(int width, int height, Bitmap.Config config) {
        layout(width, height);
        
        // Set up our background bitmap.
        setBackground(width, height);
    }
    

    /**
     * We are starting the animation loop.  The screen size is known.
     * 
     * <p>doUpdate() and doDraw() may be called from this point on.
     * 
     * <p>If subclasses override this, they must call through to this method.
     */
    @Override
    protected void animStart() {
        // Make arrays of the instruments and gauges for faster access.
        instrumentArray = new Instrument[instruments.size()];
        instruments.toArray(instrumentArray);
        gaugeArray = new Gauge[gauges.size()];
        gauges.toArray(gaugeArray);

        // Start measurement on all the instruments.
        for (int i = 0; i < instrumentArray.length; ++i)
            instrumentArray[i].measureStart();
    }


    /**
     * We are stopping the animation loop, for example to pause the app.
     * 
     * <p>doUpdate() and doDraw() will not be called from this point on.
     * 
     * <p>If subclasses override this, they must call through to this method.
     */
    @Override
    protected void animStop() {
        // Stop measurement on all the instruments.
        for (int i = 0; i < instrumentArray.length; ++i)
            instrumentArray[i].measureStop();
    }
    

    /**
     * The application is closing down.  Clean up any resources.
     */
    @Override
    protected void appStop() {
        for (Instrument i : instruments)
            i.appStop();
    }


    // ******************************************************************** //
    // Graphics Setup.
    // ******************************************************************** //

    /**
     * Set up our background bitmap.
     * 
     * @param   width       The width of the surface.
     * @param   height      The height of the surface.
     */
    private void setBackground(int width, int height) {
        if (optionSet(SURFACE_CACHE_BG)) {
            // Create the bitmap for the background,
            // and the Canvas for drawing into it.
            backgroundBitmap = getBitmap(width, height);
            backgroundCanvas = new Canvas(backgroundBitmap);

            // Get the gauges to draw their backgrounds in to the bitmap.
            backgroundCanvas.drawColor(0xff000000);
            for (Gauge g : gauges)
                g.drawBackground(backgroundCanvas);
        } else {
            backgroundBitmap = null;
            backgroundCanvas = null;
        }
    }

    
    // ******************************************************************** //
    // Main Processing Loop.
    // ******************************************************************** //

    /**
     * Update the state of the application for the current frame.
     * 
     * <p>Applications must override this, and can use it to update
     * for example the physics of a game.  This may be a no-op in some cases.
     * 
     * <p>doDraw() will always be called after this method is called;
     * however, the converse is not true, as we sometimes need to draw
     * just to update the screen.  Hence this method is useful for
     * updates which are dependent on time rather than frames.
     * 
     * @param   now         Nominal time of the current frame in ms.
     */
    @Override
    protected void doUpdate(long now) {
        final int il = instrumentArray.length;
        for (int i = 0; i < il; ++i)
            instrumentArray[i].doUpdate(now);
    }


    /**
     * Draw the current frame of the application.
     * 
     * <p>Applications must override this, and are expected to draw the
     * entire screen into the provided canvas.
     * 
     * <p>This method will always be called after a call to doUpdate(),
     * and also when the screen needs to be re-drawn.
     * 
     * @param   canvas      The Canvas to draw into.
     * @param   now         Nominal time of the current frame in ms.
     */
    @Override
    protected void doDraw(Canvas canvas, long now) {
        // Draw the background on to the screen.
        if (backgroundBitmap != null)
            canvas.drawBitmap(backgroundBitmap, 0, 0, null);
        else
            canvas.drawColor(0xff000000);
        final boolean needBg = backgroundBitmap == null;
        
        // Draw the gauges over the background.
        final int gl = gaugeArray.length;
        for (int g = 0; g < gl; ++g)
            gaugeArray[g].draw(canvas, now, needBg);
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

	// The instruments and gauges associated with this surface.
	private ArrayList<Instrument> instruments = null;
    private ArrayList<Gauge> gauges = null;

    // The lists of instruments and gauges in array form for fast access.
    private Instrument[] instrumentArray = null;
    private Gauge[] gaugeArray = null;
    
    // Bitmap in which we draw the audio waveform display,
    // and the Canvas and Paint for drawing into it.  Only used if
    // OPTION_CACHE_BG is set; otherwise null.
    private Bitmap backgroundBitmap = null;
    private Canvas backgroundCanvas = null;

}

