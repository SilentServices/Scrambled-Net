
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
import android.graphics.Typeface;


/**
 * A graphical display which shows some data in a region
 * within a view.  The data may come from an {@link Instrument} or
 * some other source.
 * 
 * <h2>Configuration</h2>
 * 
 * <p>Your gauge will be notified of its geometry by a call to
 * {@link #setGeometry(Rect)}.  This is where you should note your position
 * and size and perform any internal layout you need to do.
 * 
 * <p>Note that if you are running in an app which handles screen
 * configuration changes, {@link #setGeometry(Rect)} will be called any
 * time the screen changes size or shape (e.g. on an orientation change).
 * You should be prepared to handle these subsequent calls by re-creating
 * data structures, re-doing layout, etc., as required.
 * 
 * <h2>Data Updating</h2>
 * 
 * <p>It is assumed that your gauge has some kind of data source, but
 * how this works is up to you.
 * 
 * <h2>Drawing Sequence -- User</h2>
 * 
 * <p>A gauge may have a background which is rendered separately from
 * its content, for performance reasons.  Hence, a Gauge user must request
 * the background to be drawn, and then the gauge content to be drawn.  If
 * the caller is going to cache the background, the background need be
 * requested only when the geometry changes.
 * 
 * <p>There are two options.  In the non-caching case, the caller may
 * simply call {@link #draw(Canvas, long, boolean)}, passing true
 * as the last argument.  This asks the Gauge to draw its background and
 * its content.
 * 
 * <p>In the caching case, the caller should call
 * {@link #drawBackground(Canvas)} to ask the gauge to draw its
 * background into the given canvas.  Since the gauge will use the same
 * co-ordinates that it uses to draw to the screen, the canvas will need
 * to be the size of the screen (or you can translate the co-ordinates).
 * Then, to draw the gauge, the caller should render the stored background
 * and then call {@link #draw(Canvas, long, boolean)}.
 * 
 * <h2>Drawing Sequence -- Implementor</h2>
 * 
 * <p>From the Gauge implementor's point of view, there are two routines
 * to implement: {@link #drawBackgroundBody(Canvas, Paint)} (optional), and
 * {@link #drawBody(Canvas, Paint, long)}.
 * 
 * <p>If your implementation of {@link #drawBody(Canvas, Paint, long)}
 * draws a complete, opaque rendition of the gauge, that's all you need;
 * there's no need to provide an implementation of drawBackgroundBody().
 * But if your gauge has a separate, persistent background appearance,
 * you may reap a performance benefit by separating out its drawing.
 * Do this by implementing {@link #drawBackgroundBody(Canvas, Paint)}.
 * This routine should draw the gauge background at the gauge's
 * configured position in the specified Canvas.
 * 
 * <p>A facility is provided for caching background images.  To use this,
 * call {@link #cacheBackground()} once your layout is set up -- for example
 * at the end of {@link #setGeometry(Rect)}.  At that point, your background
 * will be fetched (by calling your implementation of drawBackgroundBody())
 * and stored; then when someone asks us to draw our background, the request
 * will be satisfied using the stored bitmap, without calling your
 * drawBackgroundBody() again.
 */
public class Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
     */
	public Gauge(SurfaceRunner parent) {
		this(parent, 0);
	}


    /**
     * Set up this view.
     * 
     * @param   parent          Parent surface.
     * @param   options         Options for this SurfaceRunner.  A bitwise OR of
     *                          GAUGE_XXX constants.
     */
    public Gauge(SurfaceRunner parent, int options) {
        parentSurface = parent;
        gaugeOptions = options;
        
        // Set up our paint.
        drawPaint = new Paint();

        initializePaint(drawPaint);
    }

	
	/**
	 * Set up this view.
	 * 
     * @param   parent          Parent surface.
     * @param	grid			Colour for drawing a data scale / grid.
     * @param	plot			Colour for drawing data plots.
	 */
	public Gauge(SurfaceRunner parent, int grid, int plot) {
        this(parent, 0, grid, plot);
	}

    
    /**
     * Set up this view.
     * 
     * @param   parent          Parent surface.
     * @param   options         Options for this SurfaceRunner.  A bitwise OR of
     *                          GAUGE_XXX constants.
     * @param   grid            Colour for drawing a data scale / grid.
     * @param   plot            Colour for drawing data plots.
     */
    public Gauge(SurfaceRunner parent, int options, int grid, int plot) {
        parentSurface = parent;
        gaugeOptions = options;
        gridColour = grid;
        plotColour = plot;
     
        // Set up our paint.
        drawPaint = new Paint();

        initializePaint(drawPaint);
    }


	/**
	 * Set up the paint for this element.  This is called during
	 * initialization.  Subclasses can override this to do class-specific
	 * one-time initialization.
	 * 
	 * @param paint			The paint to initialize.
	 */
	protected void initializePaint(Paint paint) { }
	

    // ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    /**
     * Check whether the given option flag is set on this surface.
     * 
     * @param   option      The option flag to test; one of GAUGE_XXX.
     * @return              true iff the option is set.
     */
    public boolean optionSet(int option) {
        return (gaugeOptions & option) != 0;
    }
    

    // ******************************************************************** //
    // Global Layout Parameters.
    // ******************************************************************** //
	
    /**
     * Set the default font for all text.
     * 
     * @param   face        The default font for all text.
     */
    public static void setTextTypeface(Typeface face) {
        baseTextFace = face;
    }


    /**
     * Get the default font for all text.
     * 
     * @return              The default font for all text.
     */
    public static Typeface getTextTypeface() {
        return baseTextFace;
    }


    /**
     * Set the base size for text.
     * 
     * @param   size        Base text size for the app.
     */
	public static void setBaseTextSize(float size) {
        baseTextSize = size;
        headTextSize = baseTextSize * 1.3f;
        miniTextSize = baseTextSize * 0.9f;
        tinyTextSize = baseTextSize * 0.8f;
    }


    /**
     * Get the base size for text.
     * 
     * @return              Base text size for the app.
     */
	public static float getBaseTextSize() {
        return baseTextSize;
    }


    /**
     * Set the size for header text.
     * 
     * @param   size        Header text size for the app.
     */
    public static void setHeadTextSize(float size) {
        headTextSize = size;
    }


    /**
     * Get the size for header text.
     * 
     * @return              Header text size for the app.
     */
    public static float getHeadTextSize() {
        return headTextSize;
    }


    /**
     * Set the size for mini text.
     * 
     * @param   size        Mini text size for the app.
     */
    public static void setMiniTextSize(float size) {
        miniTextSize = size;
    }


    /**
     * Get the size for mini text.
     * 
     * @return              Mini text size for the app.
     */
	public static float getMiniTextSize() {
        return miniTextSize;
    }


    /**
     * Set the size for tiny text.
     * 
     * @param   size        Tiny text size for the app.
     */
    public static void setTinyTextSize(float size) {
        tinyTextSize = size;
    }


    /**
     * Get the size for tiny text based on this screen's size.
     * 
     * @return              Tiny text size for the app.
     */
	public static float getTinyTextSize() {
        return tinyTextSize;
    }


    /**
     * Set the horizontal scaling of the font; this can be used to
     * produce a tall, thin font.
     * 
     * @param   scale       Horizontal scaling of the font.
     */
    public static void setTextScaleX(float scale) {
        textScaleX = scale;
    }


    /**
     * Get the base size for text based on this screen's size.
     * 
     * @return              Horizontal scaling of the font.
     */
    public static float getTextScaleX() {
        return textScaleX;
    }


    /**
     * Set the sidebar width.
     * 
     * @param   width       The sidebar width.
     */
	public static void setSidebarWidth(int width) {
        viewSidebar = width;
    }

    
    /**
     * Get the sidebar width.
     * 
     * @return              The sidebar width.
     */
	public static int getSidebarWidth() {
        return viewSidebar;
    }


    /**
     * Set the amount of padding between major elements in a view.
     * 
     * @param   pad     The amount of padding between major elements in a view.
     */
	public static void setInterPadding(int pad) {
        interPadding = pad;
    }


    /**
     * Get the amount of padding between major elements in a view.
     * 
     * @return          The amount of padding between major elements in a view.
     */
	public static int getInterPadding() {
        return interPadding;
    }


    /**
     * Set the amount of padding within atoms within an element.  Specifically
     * the small gaps in side bars.
     * 
     * @param   gap     The amount of padding within atoms within an element
     */
	public static void setInnerGap(int gap) {
        innerGap = gap;
    }


    /**
     * Get the amount of padding within atoms within an element.  Specifically
     * the small gaps in side bars.
     * 
     * @return          The amount of padding within atoms within an element
     */
	public static int getInnerGap() {
        return innerGap;
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
	public void setGeometry(Rect bounds) {
		elemBounds = bounds;
		
		// Any cached background we may have is now invalid.
		backgroundBitmap = null;
		backgroundCanvas = null;
	}
	
	
	/**
	 * Fetch and cache an image of the background now, then use that
	 * to draw the background on future draw requests.  The cached image
	 * is invalidated the next time the geometry changes.
	 * 
	 * <p>Implementations should call this method once their layout is
	 * set -- for example at the end of {@link #setGeometry(Rect)} --
	 * if they have a significant static background that they wish
	 * to have cached.
	 */
	protected void cacheBackground() {
        // Create the bitmap for the background,
        // and the Canvas for drawing into it.
        backgroundBitmap = getSurface().getBitmap(elemBounds.width(),
                                                  elemBounds.height());
        backgroundCanvas = new Canvas(backgroundBitmap);
        
        // Because the element is going to draw its BG at its proper
        // location, and this bitmap is local to the element, we need
        // to translate the drawing co-ordinates.
        drawBackground(backgroundCanvas, -elemBounds.left, -elemBounds.top);
	}

	
	/**
	 * Get the minimum preferred width for this atom.
	 * 
	 * @return			The minimum preferred width for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	public int getPreferredWidth() {
		return 0;
	}
	

	/**
	 * Get the minimum preferred height for this atom.
	 * 
	 * @return			The minimum preferred height for this atom.
	 * 					Returns zero if we don't know yet.
	 */
	public int getPreferredHeight() {
		return 0;
	}
	

    /**
     * Determine whether we have the bounding rect of this Element.
     * 
     * @return              True if our geometry has been set up.
     */
    public final boolean haveBounds() {
        return getWidth() > 0 && getHeight() > 0;
    }


	/**
	 * Get the bounding rect of this Element.
	 * 
	 * @return				The bounding rect of this element within
	 * 						its parent View.  This will be 0, 0, 0, 0 if
	 * 						setGeometry() has not been called yet.
	 */
	public final Rect getBounds() {
		return elemBounds;
	}

	
	/**
	 * Get the width of this element -- i.e. the current configured width.
	 * 
	 * @return				The width of this element within
	 * 						its parent View.  This will be 0 if
	 * 						setGeometry() has not been called yet.
	 */
	public final int getWidth() {
		return elemBounds.right - elemBounds.left;
	}
	
	
	/**
	 * Get the height of this element -- i.e. the current configured height.
	 * 
	 * @return				The height of this element within
	 * 						its parent View.  This will be 0 if
	 * 						setGeometry() has not been called yet.
	 */
	public final int getHeight() {
		return elemBounds.bottom - elemBounds.top;
	}
	

    // ******************************************************************** //
    // Appearance.
    // ******************************************************************** //

	/**
	 * Set the background colour of this element.
	 * 
	 * @param	col			The new background colour, in ARGB format.
	 */
	public void setBackgroundColor(int col) {
		colBg = col;
	}
	

	/**
	 * Get the background colour of this element.
	 * 
	 * @return				The background colour, in ARGB format.
	 */
	public int getBackgroundColor() {
		return colBg;
	}
	

	/**
	 * Set the plot colours of this element.
	 * 
     * @param	grid      Colour for drawing a data scale / grid.
     * @param	plot      Colour for drawing data plots.
	 */
	public void setDataColors(int grid, int plot) {
		gridColour = grid;
		plotColour = plot;
	}
	

    /**
     * Set the data scale / grid colour of this element.
     * 
     * @param   grid      Colour for drawing a data scale / grid.
     */
    public void setGridColor(int grid) {
        gridColour = grid;
    }
    

    /**
     * Set the data plot colour of this element.
     * 
     * @param   plot      Colour for drawing a data plot.
     */
    public void setPlotColor(int plot) {
        plotColour = plot;
    }
    

    /**
     * Get the data scale / grid colour of this element.
     * 
     * @return            Colour for drawing a data scale / grid.
     */
    public int getGridColor() {
        return gridColour;
    }
    

    /**
     * Get the data plot colour of this element.
     * 
     * @return          Colour for drawing data plots.
     */
    public int getPlotColor() {
        return plotColour;
    }
    

	// ******************************************************************** //
	// Error Handling.
	// ******************************************************************** //

    /**
     * An error has occurred.  Notify the user somehow.
     * 
     * <p>Subclasses can override this to do something neat.
     * 
     * @param   error       ERR_XXX code describing the error.
     */
    public void error(int error) {
        
    }

    
    // ******************************************************************** //
    // View Drawing.
    // ******************************************************************** //
    

	/**
	 * Get this element's Paint.
	 * 
	 * @return				The Paint which was set up in initializePaint().
	 */
	protected Paint getPaint() {
		return drawPaint;
	}
	
	   
    /**
     * This method is called to ask the element to draw its static
     * content; i.e. the background / chrome.
     * 
     * @param   canvas      Canvas to draw into.
     */
    public void drawBackground(Canvas canvas) {
        if (backgroundBitmap != null)
            canvas.drawBitmap(backgroundBitmap, elemBounds.left, elemBounds.top, null);
        else
            drawBackground(canvas, 0, 0);
    }

    
    /**
     * This internal method is used to get the gauge implementation to
     * render its background at a specific location.
     * 
     * @param   canvas      Canvas to draw into.
     * @param   dx          X co-ordinate translation to apply.
     * @param   dy          Y co-ordinate translation to apply.
     */
    private void drawBackground(Canvas canvas, int dx, int dy) {
        // Clip to our part of the canvas.
        canvas.save();
        canvas.translate(dx, dy);
        canvas.clipRect(getBounds());
        
        drawBackgroundBody(canvas, drawPaint);
        
        canvas.restore();
    }

   
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
    protected void drawBackgroundBody(Canvas canvas, Paint paint) {
        // If not overridden, we shouldn't need anything, as the overall
        // background is cleared each time.
    }
    

    /**
     * This method is called to ask the element to draw its dynamic content.
     * 
     * @param   canvas      Canvas to draw into.
     * @param   now         Nominal system time in ms. of this update.
     * @param   bg          Iff true, tell the gauge to draw its background
     *                      first.  This is cheaper than calling
     *                      {@link #drawBackground(Canvas)} before
     *                      this method.
     */
    public void draw(Canvas canvas, long now, boolean bg) {
		drawStart(canvas, drawPaint, now);
		if (bg) {
	        if (backgroundBitmap != null)
	            canvas.drawBitmap(backgroundBitmap,
	                              elemBounds.left, elemBounds.top, null);
	        else
	            drawBackgroundBody(canvas, drawPaint);
		}
		drawBody(canvas, drawPaint, now);
		drawFinish(canvas, drawPaint, now);
	}


	/**
	 * Do initial parts of drawing for this element.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	protected void drawStart(Canvas canvas, Paint paint, long now) {
		// Clip to our part of the canvas.
		canvas.save();
		canvas.clipRect(getBounds());
	}

	
	/**
	 * Do the subclass-specific parts of drawing for this element.
	 * 
	 * Subclasses should override this to do their drawing.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	protected void drawBody(Canvas canvas, Paint paint, long now) {
		// If not overridden, just fill with BG colour.
		canvas.drawColor(colBg);
	}
	

	/**
	 * Wrap up drawing of this element.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	protected void drawFinish(Canvas canvas, Paint paint, long now) {
		canvas.restore();
	}

	
	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	/**
	 * Get the app context of this Element.
	 * 
	 * @return             The app context we're running in.
	 */
	protected SurfaceRunner getSurface() {
		return parentSurface;
	}
	

	// ******************************************************************** //
	// Private Constants.
	// ******************************************************************** //

	// The minimum happy text size.
	private static final float MIN_TEXT = 22f;
	
	
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "instrument";

    // The default font for all text.
    private static Typeface baseTextFace = Typeface.MONOSPACE;

    // The base size for all text, based on screen size.
    private static float baseTextSize = MIN_TEXT;
    
    // Various other text sizes.
    private static float headTextSize = MIN_TEXT * 1.3f;
    private static float miniTextSize = MIN_TEXT * 0.9f;
    private static float tinyTextSize = MIN_TEXT * 0.8f;
    
    // The horizontal scaling of the font; this can be used to
    // produce a tall, thin font.
    private static float textScaleX = 1f;

    // The thickness of a side bar in a view element.
    private static int viewSidebar;
    
    // The amount of padding between major elements in a view.
    private static int interPadding;
    
    // The amount of padding within atoms within an element.  Specifically
    // the small gaps in side bars.
    private static int innerGap;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Application handle.
	private final SurfaceRunner parentSurface;

    // Option flags for this instance.  A bitwise OR of GAUGE_XXX constants.
    private int gaugeOptions = 0;

	// The paint we use for drawing.
	private Paint drawPaint = null;

	// The bounding rect of this element within its parent View.
	private Rect elemBounds = new Rect(0, 0, 0, 0);

	// Background colour.
	private int colBg = 0xff000000;

	// Colour of the graph grid and plot.
	private int gridColour = 0xff00ff00;
	private int plotColour = 0xffff0000;

    // Bitmap in which we draw the gauge background, if we're caching it,
    // and the Canvas for drawing into it.
    private Bitmap backgroundBitmap = null;
    private Canvas backgroundCanvas = null;

}

