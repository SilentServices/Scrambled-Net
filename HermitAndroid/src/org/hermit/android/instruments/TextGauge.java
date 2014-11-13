
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

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;


/**
 * A {@link Gauge} which displays data in textual form, generally as
 * a grid of numeric values.
 */
public class TextGauge
	extends Gauge
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
    /**
     * Set up this view, and configure the text fields to be displayed in
     * this element.  This is equivalent to calling setTextFields()
     * after the basic constructor.
     * 
     * We support display of a single field, or a rectangular table
     * of fields.  The caller must call
     * {@link #setTextFields(String[] template, int rows)} to set the
     * table format.
     * 
     * @param   parent          Parent surface.
     */
    public TextGauge(SurfaceRunner parent) {
        super(parent);
        textSize = getBaseTextSize();
    }


    /**
     * Set up this view, and configure the text fields to be displayed in
     * this element.  This is equivalent to calling setTextFields()
     * after the basic constructor.
     * 
     * We support display of a single field, or a rectangular table
     * of fields.  The fields are specified by passing in sample text
     * values to be measured; we then allocate the space automatically.
     * 
     * @param   parent          Parent surface.
     * @param   template    Strings representing the columns to display.
     *                      Each one should be a sample piece of text
     *                      which will be measured to determine the
     *                      required space for each column.  Must be provided.
     * @param   rows        Number of rows of text to display.
     */
    public TextGauge(SurfaceRunner parent, String[] template, int rows) {
        super(parent);
        textSize = getBaseTextSize();
        
        // Set up the text fields.
        setTextFields(template, rows);
    }


	/**
	 * Set up the paint for this element.  This is called during
	 * initialisation.  Subclasses can override this to do class-specific
	 * one-time initialisation.
	 * 
	 * @param paint			The paint to initialise.
	 */
	@Override
	protected void initializePaint(Paint paint) {
	    float scaleX = getTextScaleX();
	    if (scaleX != 1f)
	        paint.setTextScaleX(scaleX);
		paint.setTypeface(getTextTypeface());
		paint.setAntiAlias(true);
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

		// Position our text based on our actual geometry.  If setTextFields()
		// hasn't been called this does nothing.
		positionText();
	}


    /**
     * Set the margins around the displayed text.  This the total space
     * between the edges of the element and the outside bounds of the text.
     * 
	 * @param	left		The left margin.
	 * @param	top			The top margin.
	 * @param	right		The right margin.
	 * @param	bottom		The bottom margin.
     */
	public void setMargins(int left, int top, int right, int bottom) {
		marginLeft = left;
		marginTop = top;
		marginRight = right;
		marginBottom = bottom;
		
		// Position our text based on these new margins.  If setTextFields()
		// hasn't been called this does nothing.
		positionText();
	}


	/**
	 * Set up the text fields to be displayed in this element.
	 * If this is never called, there will be no text.
	 * 
	 * We support display of a single field, or a rectangular table
	 * of fields.  The fields are specified by passing in sample text
	 * values to be measured; we then allocate the space automatically.
	 * 
	 * This must be called before setText() can be called.
	 * 
	 * @param	template	Strings representing the columns to display.
	 * 						Each one should be a sample piece of text
	 * 						which will be measured to determine the
	 * 						required space for each column.
     * @param   rows        Number of rows of text to display.
	 */
	public void setTextFields(String[] template, int rows) {
		fieldTemplate = template;
        numRows = rows;

        // Make the field buffers based on the template.
        char[][][] buffers = new char[numRows][][];
        for (int r = 0; r < numRows; ++r) {
            int cols = template.length;
            buffers[r] = new char[cols][];
            for (int c = 0; c < cols; ++c) {
                int l = template[c].length();
                char[] buf = new char[l];
                for (int i = 0; i < l; ++i)
                    buf[i] = ' ';
                buffers[r][c] = buf;
            }
        }
        fieldBuffers = buffers;

		// Position our text based on the template.  If setGeometry()
		// hasn't been called yet, then the positions will not be final,
		// but getTextWidth() and getTextHeight() will return sensible
		// values.
		positionText();
	}
	
	
	/**
	 * Get the text buffers for the field values.  The caller can change
	 * a field's content by writing to the appropriate member of the
	 * array, as in "buffer[row][col][0] = 'X';".
	 * 
	 * @return             Text buffers for the field values.
	 */
	public char[][][] getBuffer() {
	    return fieldBuffers;
	}
	
	
	/**
	 * Get the minimum width needed to fit all the text.
	 * 
	 * @return			The minimum width needed to fit all the text.
	 * 					Returns zero if setTextFields() hasn't been called.
	 */
	@Override
	public int getPreferredWidth() {
		return textWidth;
	}
	

	/**
	 * Get the minimum height needed to fit all the text.
	 * 
	 * @return			The minimum height needed to fit all the text.
	 * 					Returns zero if setTextFields() hasn't been called.
	 */
	@Override
	public int getPreferredHeight() {
		return textHeight;
	}


	/**
	 * Position the text based on the current template and geometry.
	 * If If setTextFields() hasn't been called this does nothing.
	 * If setGeometry() hasn't been called yet, then the positions will
	 * not be final, but getTextWidth() and getTextHeight() will return
	 * sensible values.
	 */
	private void positionText() {
		if (fieldTemplate == null)
			return;
		
		final int nf = fieldTemplate.length;
		colsX = new int[nf];
		rowsY = new int[numRows];
		
		Rect bounds = getBounds();
		Paint paint = getPaint();
		paint.setTextSize(textSize);

		// Assign all the column positions based on minimum width.
		int x = bounds.left;
		for (int i = 0; i < nf; ++i) {
			int len = (int) Math.ceil(paint.measureText(fieldTemplate[i]));
			int lp = i > 0 ? textPadLeft : marginLeft;
			int rp = i < nf - 1 ? textPadRight : marginRight;
			colsX[i] = x + lp;
			x += len + lp + rp;
		}
		textWidth = x - bounds.left;
		
		// If we have excess width, distribute it into the inter-column gaps.
		// Don't adjust textWidth because it is the minimum.
		if (nf > 1) {
			int excess = (bounds.right - x) / (nf - 1);
			if (excess > 0) {
				for (int i = 1; i < nf; ++i)
					colsX[i] += excess * i;
			}
		}
		
		// Assign all the row positions based on minimum height.
 	   	Paint.FontMetricsInt fm = paint.getFontMetricsInt();
		int y = bounds.top;
		for (int i = 0; i < numRows; ++i) {
			int tp = i > 0 ? textPadTop : marginTop;
			int bp = i < numRows - 1 ? textPadBottom : marginBottom;
			rowsY[i] = y + tp - fm.ascent - 2;
			y += -fm.ascent - 2 + fm.descent + tp + bp;
		}
		textHeight = y - bounds.top;
	}
	
	
    // ******************************************************************** //
	// Appearance.
	// ******************************************************************** //

	/**
	 * Set the text colour of this element.
	 * 
	 * @param	col			The new text colour, in ARGB format.
	 */
	public void setTextColor(int col) {
		setPlotColor(col);
	}
	

	/**
	 * Get the text colour of this element.
	 * 
	 * @return				The text colour, in ARGB format.
	 */
	public int getTextColor() {
		return getPlotColor();
	}
	

	/**
	 * Set the text size of this element.
	 * 
	 * @param	size		The new text size.
	 */
	public void setTextSize(float size) {
		textSize = size;
		
		// Position our text based on the new size.  If setTextFields()
		// hasn't been called this does nothing.
		positionText();
	}
	

	/**
	 * Get the text size of this element.
	 * 
	 * @return				The text size.
	 */
	public float getTextSize() {
		return textSize;
	}
	

	// ******************************************************************** //
	// View Drawing.
	// ******************************************************************** //
	
	/**
	 * This method is called to ask the element to draw itself.
	 * 
	 * @param	canvas		Canvas to draw into.
	 * @param	paint		The Paint which was set up in initializePaint().
     * @param   now         Nominal system time in ms. of this update.
	 */
	@Override
	protected void drawBody(Canvas canvas, Paint paint, long now) {
		// Set up the display style.
		paint.setColor(getPlotColor());
		paint.setTextSize(textSize);

		final char[][][] tv = fieldBuffers;
		
		// If we have any text to show, draw it.
		if (tv != null) {
			for (int row = 0; row < rowsY.length && row < tv.length; ++row) {
			    char[][] fields = tv[row];
				int y = rowsY[row];
				for (int col = 0; col < colsX.length && col < fields.length; ++col) {
				    char[] field = fields[col];
					int x = colsX[col];
					canvas.drawText(field, 0, field.length, x, y, paint);
				}
			}
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
	
	// Template for the text fields we're displaying.
	private String[] fieldTemplate = null;
	private int numRows = 0;
	
	// Buffers where the values of the fields will be stored.
    private char[][][] fieldBuffers;

	// Horizontal positions of the text columns, and vertical positions
	// of the rows.  These are the actual text base positions.  These
	// will be null if we have no defined text fields.
	private int[] colsX = null;
	private int[] rowsY = null;
	
	// The width and height we would need to display all the text at minimum,
	// including padding and margins.  Set after a call to setTextFields().
	private int textWidth = 0;
	private int textHeight = 0;
	
	// Current text size.
	private float textSize;

	// Margins.  These are applied around the outside of the text.
	private int marginLeft = 0;
	private int marginRight = 0;
	private int marginTop = 0;
	private int marginBottom = 0;

	// Text padding.  This is applied between all pairs of text fields.
	private int textPadLeft = 2;
	private int textPadRight = 2;
	private int textPadTop = 0;
	private int textPadBottom = 0;
	
}

