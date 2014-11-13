
/**
 * widgets: useful add-on widgets for Android.
 *
 *   This program is free software; you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License version 2
 *   as published by the Free Software Foundation (see COPYING).
 * 
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 */


package org.hermit.android.widgets;


import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.EditText;


/**
 * A custom EditText that draws lines between each line of text
 * that is displayed.
 */
public class LinedEditText extends EditText {

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

    /**
     * Construct a widget from a given attribute set.  This is required to
     * allow this widget to be used from XML layouts.
     * 
     * @param	context			Context we're running in.
     * @param	attrs			Attributes for this widget.
     */
    public LinedEditText(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        drawRect = new Rect();
        
        // Initialise the painter with the drawing attributes for the lines.
        drawPaint = new Paint();
        drawPaint.setStyle(Paint.Style.STROKE);
        drawPaint.setColor(LINE_COLOUR);
    }
    
	  
	// ******************************************************************** //
	// Drawing.
	// ******************************************************************** //

    /**
     * Overridden onDraw method.  Draw the text, with lines.
     * 
     * @param	canvas			Canvas to draw into.
     */
    @Override
    protected void onDraw(Canvas canvas) {
        Rect r = drawRect;
        Paint paint = drawPaint;

        // Draw a line under each text line.
        int count = getLineCount();
        for (int i = 0; i < count; i++) {
        	// Get the bounds for this text line, and draw a line under it.
            int baseline = getLineBounds(i, r);
            canvas.drawLine(r.left, baseline + 1, r.right, baseline + 1, paint);
        }

        super.onDraw(canvas);
    }
    
	  
	// ******************************************************************** //
	// Private Constants.
	// ******************************************************************** //

    // Colour for the text lines.
    private static final int LINE_COLOUR = 0x600000ff;
    
	  
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

    // Rect used for storing bounds.
    private Rect drawRect;
    
    // Paint used for drawing the lines.
    private Paint drawPaint;
    
}

