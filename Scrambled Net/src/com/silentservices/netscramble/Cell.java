/**
 * NetScramble: unscramble a network and connect all the terminals.
 * The player is given a network diagram with the parts of the network
 * randomly rotated; he/she must rotate them to connect all the terminals
 * to the server.
 * 
 * This is an Android implementation of the KDE game "knetwalk" by
 * Andi Peredri, Thomas Nagy, and Reinhold Kainhofer.
 *
 * © 2007-2010 Ian Cameron Smith <johantheghost@yahoo.com>
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

package com.silentservices.netscramble;

import java.security.SecureRandom;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Bundle;

/**
 * This class implements a cell in the game board. It handles the logic and
 * state of the cell, and implements the visible view of the cell.
 */
class Cell {

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * Define the connected direction combinations. The enum is carefully
	 * organised so that the ordinal() of each value is a bitmask representing
	 * the connected directions. This allows us to manipulate directions more
	 * easily.
	 * 
	 * Each enum value also stores the ID of the bitmap representing it, or zero
	 * if none. Note that this is the bitmap for the cabling layer, not the
	 * background or foreground (terminal etc).
	 */
	enum Dir {
		FREE(0), // Unconnected cell.
		___L(R.drawable.cable0001), __D_(R.drawable.cable0010), __DL(
				R.drawable.cable0011), _R__(R.drawable.cable0100), _R_L(
				R.drawable.cable0101), _RD_(R.drawable.cable0110), _RDL(
				R.drawable.cable0111), U___(R.drawable.cable1000), U__L(
				R.drawable.cable1001), U_D_(R.drawable.cable1010), U_DL(
				R.drawable.cable1011), UR__(R.drawable.cable1100), UR_L(
				R.drawable.cable1101), URD_(R.drawable.cable1110), URDL(
				R.drawable.cable1111), NONE(0); // Not a cell.

		Dir(int img) {
			imageId = img;
		}

		private static Dir getDir(int bits) {
			return dirs[bits];
		}

		static final Dir[] dirs = values();
		static final Dir[] cardinals = { ___L, __D_, _R__, U___ };
		static final int[][] cardinalOffs = { { -1, 0 }, // ___L
				{ 0, 1 }, // __D_
				{ 1, 0 }, // _R__
				{ 0, -1 }, // U___
		};

		// The direction which is the reverse of this one.
		Dir reverse = null;
		static {
			U___.reverse = __D_;
			_R__.reverse = ___L;
			__D_.reverse = U___;
			___L.reverse = _R__;
		}

		final int imageId;

		private Bitmap normalImg = null;
		private Bitmap greyImg = null;
	}

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up this cell.
	 * 
	 * @param parent
	 *            Parent application context.
	 * @param board
	 *            This cell's parent board.
	 * @param x
	 *            This cell's x-position in the board grid.
	 * @param y
	 *            This cell's y-position in the board grid.
	 */
	Cell(NetScramble parent, BoardView board, int x, int y) {
		xindex = x;
		yindex = y;

		// Create the temp. objects used in drawing.
		cellLeft = 0;
		cellTop = 0;
		cellWidth = 0;
		cellHeight = 0;
		cellPaint = new Paint();

		// Reset the cell's state.
		reset(Dir.NONE);
	}

	// ******************************************************************** //
	// Image Setup.
	// ******************************************************************** //

	/**
	 * Initialise the pixmaps used by the Cell class.
	 * 
	 * @param res
	 *            Handle on the application resources.
	 * @param width
	 *            The cell width.
	 * @param height
	 *            The cell height.
	 * @param config
	 *            The pixel format of the surface.
	 */
	static void initPixmaps(Resources res, int width, int height,
			Bitmap.Config config) {
		// Load all the cable pixmaps.
		for (Dir d : Dir.dirs) {
			if (d.imageId == 0)
				continue;

			// Load the pixmap for this cable configuration. Scale it to the
			// right size.
			Bitmap base = BitmapFactory.decodeResource(res, d.imageId);
			Bitmap pixmap = Bitmap
					.createScaledBitmap(base, width, height, true);
			d.normalImg = pixmap;

			// Create a greyed-out version of the image for the
			// disconnected version of the node.
			d.greyImg = greyOut(pixmap, config);
		}

		// Load the other pixmaps we use.
		for (Image i : Image.values()) {
			Bitmap base = BitmapFactory.decodeResource(res, i.resid);
			Bitmap pixmap = Bitmap
					.createScaledBitmap(base, width, height, true);
			i.bitmap = pixmap;
		}
	}

	/**
	 * Create a greyed-out version of the given pixmap.
	 * 
	 * @param pixmap
	 *            Base pixmap.
	 * @param config
	 *            The pixel format of the surface.
	 * @return Greyed-out version of this pixmap.
	 */
	private static Bitmap greyOut(Bitmap pixmap, Bitmap.Config config) {
		// Get the pixel data from the pixmap.
		int w = pixmap.getWidth();
		int h = pixmap.getHeight();
		int[] pixels = new int[w * h];
		pixmap.getPixels(pixels, 0, w, 0, 0, w, h);

		// Grey-out the image in the pixel data.
		for (int y = 0; y < h; ++y) {
			for (int x = 0; x < w; ++x) {
				int pix = pixels[y * w + x];
				int r = (3 * Color.red(pix)) / 5 + 100;
				int g = (3 * Color.green(pix)) / 5 + 100;
				int b = (3 * Color.blue(pix)) / 5 + 100;
				pixels[y * w + x] = Color.argb(Color.alpha(pix), r, g, b);
			}
		}

		// Create and return a pixmap from the greyed-out pixel data.
		return Bitmap.createBitmap(pixels, w, h, Bitmap.Config.ARGB_8888);
	}

	// ******************************************************************** //
	// Public Methods.
	// ******************************************************************** //

	/**
	 * Reset the state of this cell, and set the cell's isConnected directions
	 * to the given value.
	 * 
	 * @param d
	 *            Connection directions to set for the cell.
	 */
	void reset(Dir d) {
		connectedDirs = d;
		isConnected = false;
		isFullyConnected = false;
		isRoot = false;
		isLocked = false;
		isBlind = false;
		rotateTarget = 0;
		rotateStart = 0;
		rotateAngle = 0;
		highlightOn = false;
		highlightStart = 0;
		highlightPos = 0;
		blipsIncoming = 0;
		blipsOutgoing = 0;
		blipsTransfer = 0;
		haveFocus = false;

		invalidate();
	}

	// ******************************************************************** //
	// Geometry.
	// ******************************************************************** //

	/**
	 * This is called during layout when the size of this view has changed. This
	 * is where we first discover our window size, so set our geometry to match.
	 * 
	 * @param left
	 *            Left X co-ordinate of this cell in the view.
	 * @param top
	 *            Top Y co-ordinate of this cell in the view.
	 * @param width
	 *            Current width of this view.
	 * @param height
	 *            Current height of this view.
	 */
	protected void setGeometry(int left, int top, int width, int height) {
		cellLeft = left;
		cellTop = top;
		cellWidth = width;
		cellHeight = height;
		invalidate();
	}

	// ******************************************************************** //
	// Basic Cell Info.
	// ******************************************************************** //

	/**
	 * Get the x-position of this cell in the game board.
	 * 
	 * @return The x-position of this cell in the game board.
	 */
	int x() {
		return xindex;
	}

	/**
	 * Get the y-position of this cell in the game board.
	 * 
	 * @return The y-position of this cell in the game board.
	 */
	int y() {
		return yindex;
	}

	// ******************************************************************** //
	// Neighbouring Cell Tracking.
	// ******************************************************************** //

	/**
	 * Set the cell's neighbours in the game matrix. A neighbour may be null if
	 * there is no neighbour in that direction. If wrapping is enabled, the
	 * neighbour setup should reflect this.
	 * 
	 * This information can change between games, due to board size and wrapping
	 * changes.
	 * 
	 * @param u
	 *            Neighbouring cell up from this one.
	 * @param d
	 *            Neighbouring cell down from this one.
	 * @param l
	 *            Neighbouring cell left from this one.
	 * @param r
	 *            Neighbouring cell right from this one.
	 */
	void setNeighbours(Cell u, Cell d, Cell l, Cell r) {
		nextU = u;
		nextD = d;
		nextL = l;
		nextR = r;
	}

	/**
	 * Get the neighbouring cell in the given direction from this cell.
	 * 
	 * @param d
	 *            The direction to look in.
	 * @return The next cell in the given direction; may be null, or may
	 *         actually be at the other edge of the board if wrapping is on.
	 */
	Cell next(Dir d) {
		switch (d) {
		case U___:
			return nextU;
		case _R__:
			return nextR;
		case __D_:
			return nextD;
		case ___L:
			return nextL;
		default:
			throw new RuntimeException("Cell.next() called with bad dir");
		}
	}

	// ******************************************************************** //
	// Connection State.
	// ******************************************************************** //

	/**
	 * Return the directions that this cell is connected to, outwards (ie.
	 * ignoring whether there is a matching inward connection in the next cell).
	 * 
	 * @return The directions that this cell is connected to, outwards.
	 */
	Dir dirs() {
		return connectedDirs;
	}

	/**
	 * Return the directions that this cell would be connected to, outwards, if
	 * it was rotated in the given direction.
	 * 
	 * @param a
	 *            The angle in degrees to rotate to; clockwise positive.
	 * @return The directions that this cell is connected to, outwards.
	 */
	Dir rotatedDirs(int a) {
		int bits = connectedDirs.ordinal();

		if (a == 90)
			bits = ((bits & 0x01) << 3) | ((bits & 0x0e) >> 1);
		else if (a == -90)
			bits = ((bits & 0x08) >> 3) | ((bits & 0x07) << 1);
		else if (a == 180 || a == -180)
			bits = ((bits & 0x0c) >> 2) | ((bits & 0x03) << 2);
		else
			return null;

		return Dir.getDir(bits);
	}

	/**
	 * Query whether this cell has a connection in the given direction(s).
	 * 
	 * @param d
	 *            Direction(s) to check.
	 * @return true iff the cell is connected in all the given directions; else
	 *         false.
	 */
	boolean hasConnection(Dir d) {
		return !isRotated()
				&& (connectedDirs.ordinal() & d.ordinal()) == d.ordinal();
	}

	/**
	 * Determine how many connections this cell has outwards (ie. ignoring
	 * whether there is a matching inward connection in the next cell).
	 * 
	 * @return The number of outward connections from this cell.
	 */
	int numDirs() {
		if (connectedDirs == Dir.NONE)
			return 0;

		int bits = connectedDirs.ordinal();
		int n = 0;
		for (int i = 0; i < 4; ++i) {
			n += bits & 0x01;
			bits >>= 1;
		}
		return n;
	}

	/**
	 * Add the given direction as a direction this cell is connected to,
	 * outwards (ie. no attempt is made to make the reciprocal connection in the
	 * other cell).
	 * 
	 * @param d
	 *            New connected direction to add for this cell.
	 */
	void addDir(Dir d) {
		int bits = connectedDirs.ordinal();
		if ((bits & d.ordinal()) == d.ordinal())
			return;

		bits |= d.ordinal();
		setDirs(Dir.getDir(bits));
	}

	/**
	 * Set the connected directions of this cell to the given value.
	 * 
	 * @param d
	 *            New connected directions for this cell.
	 */
	void setDirs(Dir d) {
		if (d == connectedDirs)
			return;
		connectedDirs = d;
		invalidate();
	}

	// ******************************************************************** //
	// Display Options.
	// ******************************************************************** //

	/**
	 * Set the "root" flag on this cell. The root cell displays the server
	 * image.
	 * 
	 * @param b
	 *            New "root" flag for this cell.
	 */
	void setRoot(boolean b) {
		if (isRoot == b)
			return;
		isRoot = b;
		invalidate();
	}

	/**
	 * Set this cell's "blind" flag. A blind cell doesn't display its
	 * connections; it does display the server or terminal if appropriate. This
	 * is used to make the game harder.
	 * 
	 * @param b
	 *            The new "blind" flag (true = blind).
	 */
	void setBlind(boolean b) {
		if (b != isBlind) {
			isBlind = b;
			invalidate();
		}
	}

	/**
	 * Rotate this cell right now -- no animation. This is used during restore,
	 * when we need to adjust the state of the board for device rotations, not
	 * during play.
	 * 
	 * @param a
	 *            The angle in degrees to rotate to; clockwise positive.
	 */
	void rotateImmediate(int a) {
		setDirs(rotatedDirs(a));
		invalidate();
	}

	/**
	 * Determine whether this cell's "locked" flag is set.
	 * 
	 * @return This cell's "locked" flag.
	 */
	boolean isLocked() {
		return isLocked;
	}

	/**
	 * Set the "locked" flag on this cell. A locked cell is marked by a
	 * highlighted background.
	 * 
	 * @param b
	 *            New "locked" flag for this cell.
	 */
	void setLocked(boolean newlocked) {
		if (isLocked == newlocked)
			return;
		isLocked = newlocked;
		invalidate();
	}

	/**
	 * Determine whether this cell's "connected" flag is set.
	 * 
	 * @return This cell's "connected" flag.
	 */
	boolean isConnected() {
		return isConnected;
	}

	/**
	 * Set this cell's "connected" flag; this determines how the cell is
	 * displayed (cables are greyed-out if not connected).
	 * 
	 * @param b
	 *            New "connected" flag for this cell.
	 */
	void setConnected(boolean b) {
		if (isConnected == b)
			return;
		isConnected = b;

		invalidate();
	}

	/**
	 * Set this cell's "fully connected" flag; this determines how the cell is
	 * displayed. For the server, this is used to indicate victory.
	 * 
	 * @param b
	 *            New "fully connected" flag for this cell.
	 */
	void setSolved(boolean b) {
		if (isFullyConnected == b)
			return;
		isFullyConnected = b;
		invalidate();
	}

	/**
	 * Set whether this cell is focused or not.
	 * 
	 * This is part of our own focus system; we don't use the system focus, as
	 * there's no way to make it wrap correctly in a dynamic layout.
	 * (focusSearch() isn't called.)
	 */
	void setFocused(boolean focused) {
		// We do our own focus highlight in onDraw(), so when the focus
		// state changes, we have to redraw.
		haveFocus = focused;
		invalidate();
	}

	/**
	 * Add a data blip on the incoming connection in the given direction, if we
	 * have one.
	 * 
	 * @param d
	 *            Direction the blip is in, from our point of view.
	 */
	void setBlip(Dir d) {
		if (hasConnection(d))
			blipsIncoming |= d.ordinal();
	}

	// ******************************************************************** //
	// Animation Handling.
	// ******************************************************************** //

	/**
	 * Move the rotation currentAngle for this cell. This changes the cell's
	 * idea of its connections, and with fractions of 90 degrees, is used to
	 * animate rotation of the cell's connections. Setting this causes the
	 * cell's connections to be drawn at the given currentAngle; beyond +/- 45
	 * degrees, the connections are changed in accordance with the direction the
	 * cell is now facing.
	 * 
	 * @param a
	 *            The angle in degrees to rotate to; clockwise positive.
	 */
	void rotate(int a) {
		rotate(a, ROTATE_DFLT_TIME);
	}

	/**
	 * Move the rotation currentAngle for this cell. This changes the cell's
	 * idea of its connections, and with fractions of 90 degrees, is used to
	 * animate rotation of the cell's connections. Setting this causes the
	 * cell's connections to be drawn at the given currentAngle; beyond +/- 45
	 * degrees, the connections are changed in accordance with the direction the
	 * cell is now facing.
	 * 
	 * @param a
	 *            The angle in degrees to rotate to; clockwise positive.
	 * @param time
	 *            Time in ms over which to do the rotation.
	 */
	void rotate(int a, long time) {
		// If we're not already rotating, set it up.
		if (rotateTarget == 0) {
			rotateStart = System.currentTimeMillis();
			rotateAngle = 0f;
			rotateTime = time;
		}

		// Add the given rotation in.
		rotateTarget += a;

		// All data blips are lost.
		blipsIncoming = 0;
		blipsOutgoing = 0;
		blipsTransfer = 0;
	}

	/**
	 * Query whether this cell is currently rotated off the orthogonal.
	 * 
	 * @return true iff the cell is not at its base angle.
	 */
	boolean isRotated() {
		return rotateTarget != 0;
	}

	/**
	 * Set the highlight state of the cell.
	 */
	void doHighlight() {
		// If one is currently running, just start over.
		highlightOn = true;
		highlightStart = System.currentTimeMillis();
		highlightPos = 0;
	}

	/**
	 * Update the state of the application for the current frame.
	 * 
	 * <p>
	 * Applications must override this, and can use it to update for example the
	 * physics of a game. This may be a no-op in some cases.
	 * 
	 * <p>
	 * doDraw() will always be called after this method is called; however, the
	 * converse is not true, as we sometimes need to draw just to update the
	 * screen. Hence this method is useful for updates which are dependent on
	 * time rather than frames.
	 * 
	 * @param now
	 *            Current time in ms.
	 * @return true if this cell changed its connection state.
	 */
	protected boolean doUpdate(long now) {
		// Flag if we changed our connection state.
		boolean changed = false;

		// If we've got a rotation going on, move it on.
		if (rotateTarget != 0) {
			// Calculate the angle based on how long we've been going.
			rotateAngle = (float) (now - rotateStart) / (float) rotateTime
					* 90f;
			if (rotateTarget < 0)
				rotateAngle = -rotateAngle;

			// If we've gone past 90 degrees, change the connected directions.
			// Rotate the directions bits (the bottom 4 bits of the ordinal)
			// right or left, as appropriate.
			if (Math.abs(rotateAngle) >= 90f) {
				Dir dir = null;
				if (rotateTarget > 0) {
					dir = rotatedDirs(90);
					if (rotateAngle >= rotateTarget)
						rotateAngle = rotateTarget = 0f;
					else {
						rotateAngle -= 90f;
						rotateTarget -= 90f;
						rotateStart += rotateTime;
					}
				} else {
					dir = rotatedDirs(-90);
					if (rotateAngle <= rotateTarget)
						rotateAngle = rotateTarget = 0f;
					else {
						rotateAngle += 90f;
						rotateTarget += 90f;
						rotateStart += rotateTime;
					}
				}
				setDirs(dir);
				changed = true;
			}

			invalidate();
		}

		// If there's a highlight showing, advance it.
		if (highlightOn) {
			// Calculate the position based on how long we've been going.
			float frac = (float) (now - highlightStart)
					/ (float) HIGHLIGHT_TIME;
			highlightPos = (int) (frac * cellWidth * 2);

			// See if we're done.
			if (highlightPos >= cellWidth * 2) {
				highlightOn = false;
				highlightStart = 0;
				highlightPos = 0;
			}

			invalidate();
		}

		return changed;
	}

	/**
	 * Move on all the data blips one step, i.e. half a cell width. This steps
	 * them from whichever connection leg they're on to the next one.
	 * 
	 * @param count
	 *            Blip generation count.
	 */
	void advanceBlips(int count) {
		// See which outgoing blips need to be transferred onto the next
		// cell. Accumulate their directions in blipsTransfer.
		blipsTransfer = 0;
		for (int c = 0; c < Dir.cardinals.length; ++c) {
			Dir d = Dir.cardinals[c];
			int ord = d.ordinal();
			if ((blipsOutgoing & ord) != 0 && hasConnection(d))
				blipsTransfer |= ord;
		}
		blipsOutgoing = 0;

		// All incoming blips get deleted, and become outgoing blips on
		// whatever directions did not have incoming blips.
		if (blipsIncoming != 0) {
			for (int c = 0; c < Dir.cardinals.length; ++c) {
				Dir d = Dir.cardinals[c];
				int ord = d.ordinal();
				if ((blipsIncoming & ord) == 0 && hasConnection(d))
					blipsOutgoing |= ord;
			}
		}
		blipsIncoming = 0;

		// If we're the server, create new outgoing blips once in a while.
		if (isRoot && count % 6 == 0) {
			for (int c = 0; c < Dir.cardinals.length; ++c) {
				Dir d = Dir.cardinals[c];
				int ord = d.ordinal();
				if (hasConnection(d))
					blipsOutgoing |= ord;
			}
		}

		// Note that we don't invalidate(). Blips are drawn directly to
		// the screen in a separate pass.
	}

	/**
	 * Pass on all blips which were outgoing onto their next cell.
	 */
	void transferBlips() {
		for (int c = 0; c < Dir.cardinals.length; ++c) {
			Dir d = Dir.cardinals[c];
			int ord = d.ordinal();
			if ((blipsTransfer & ord) != 0) {
				Cell n = next(d);
				if (n != null)
					n.setBlip(d.reverse);
			}
		}
		blipsTransfer = 0;

		// Note that we don't invalidate(). Blips are drawn directly to
		// the screen in a separate pass.
	}

	// ******************************************************************** //
	// Cell Drawing.
	// ******************************************************************** //

	/**
	 * Set this cell's state to be invalid, forcing a redraw.
	 */
	void invalidate() {
		stateValid = false;
	}

	/**
	 * This method is called to ask the cell to draw itself. Note that this
	 * draws the cell but not any data blips, which are drawn separately.
	 * 
	 * @param canvas
	 *            Canvas to draw into.
	 * @param now
	 *            Current time in ms.
	 */
	protected void doDraw(Canvas canvas, long now) {
		// Nothing to do if we're up to date.
		if (stateValid)
			return;

		final int sx = cellLeft;
		final int sy = cellTop;
		final int ex = sx + cellWidth;
		final int ey = sy + cellHeight;
		final int midx = sx + cellWidth / 2;
		final int midy = sy + cellHeight / 2;

		canvas.save();
		canvas.clipRect(sx, sy, ex, ey);
		cellPaint.setStyle(Paint.Style.STROKE);
		cellPaint.setColor(0xff000000);

		// Draw the background tile.
		{
			Image bgImage = Image.BG;
			if (connectedDirs == Dir.NONE)
				bgImage = Image.NOTHING;
			else if (connectedDirs == Dir.FREE)
				bgImage = Image.EMPTY;
			else if (isLocked)
				bgImage = Image.LOCKED;
			canvas.drawBitmap(bgImage.bitmap, sx, sy, null);
		}

		// Draw the highlight band, if active.
		if (highlightOn) {
			cellPaint.setStyle(Paint.Style.STROKE);
			cellPaint.setStrokeWidth(5f);
			cellPaint.setColor(Color.WHITE);
			if (highlightPos < cellWidth)
				canvas.drawLine(sx, sy + highlightPos, sx + highlightPos, sy,
						cellPaint);
			else {
				int hp = highlightPos - cellWidth;
				canvas.drawLine(sx + hp, ey, ex, sy + hp, cellPaint);
			}
		}

		// If we're not empty, draw the cables / equipment.
		if (connectedDirs != Dir.FREE && connectedDirs != Dir.NONE) {
			if (!isBlind) {
				// We need to rotate the drawing matrix if the cable is
				// rotated.
				canvas.save();
				if (rotateTarget != 0)
					canvas.rotate(rotateAngle, midx, midy);

				// Draw the cable pixmap.
				Bitmap pixmap = isConnected ? connectedDirs.normalImg
						: connectedDirs.greyImg;
				canvas.drawBitmap(pixmap, sx, sy, null);
				canvas.restore();
			}

			// Draw the equipment (terminal, server) if any.
			{
				Image equipImage = null;
				if (isRoot) {
					if (isFullyConnected)
						equipImage = Image.SERVER1;
					else
						equipImage = Image.SERVER;
				} else if (numDirs() == 1) {
					if (isConnected)
						equipImage = Image.COMP2;
					else
						equipImage = Image.COMP1;
				}
				if (equipImage != null)
					canvas.drawBitmap(equipImage.bitmap, sx, sy, null);
			}
		}

		// If this is the focused cell, indicate this by drawing a red
		// border around it.
		if (haveFocus) {
			cellPaint.setStyle(Paint.Style.STROKE);
			cellPaint.setColor(0x60ff0000);
			cellPaint.setStrokeWidth(cellWidth / 8);

			canvas.drawRect(sx, sy, ex - 1, ey - 1, cellPaint);
		}

		canvas.restore();

		stateValid = true;
	}

	/**
	 * This method is called to ask the cell to draw its active data blips. This
	 * happens in a separate pass, so that blips which are in transition from
	 * one cell to another don't get cropped by the drawing of the next cell.
	 * 
	 * @param canvas
	 *            Canvas to draw into.
	 * @param now
	 *            Current time in ms.
	 * @param frac
	 *            Fractional position of the data blips, if any, along whatever
	 *            connection leg they're on.
	 */
	protected void doDrawBlips(Canvas canvas, long now, float frac) {
		// Normal cable sections and the server get blips, including the
		// section of cable going into a terminal cell. Otherwise, terminals
		// get special treatment.
		if (isRoot || numDirs() > 1 || (numDirs() == 1 && frac < 0.3f))
			drawBlips(canvas, now, frac);
		else
			drawTermData(canvas, now, frac);
	}

	/**
	 * This method is called to ask the cell to draw its active data blips. This
	 * happens in a separate pass, so that blips which are in transition from
	 * one cell to another don't get cropped by the drawing of the next cell.
	 * 
	 * @param canvas
	 *            Canvas to draw into.
	 * @param now
	 *            Current time in ms.
	 * @param frac
	 *            Fractional position of the data blips, if any, along whatever
	 *            connection leg they're on.
	 */
	private void drawBlips(Canvas canvas, long now, float frac) {
		// We don't check stateValid. Blips are always drawn.

		// But if this cell's wiring is invisible, then its blips need
		// to be too.
		if (isBlind)
			return;

		final int sx = cellLeft;
		final int sy = cellTop;

		// Note that we don't do any clipping. A blip which is passing
		// from one cell to another needs to be drawn overlapping both
		// cells.

		// Now draw in all blips. We use "glow-in" / "glow-out" images
		// for the server; otherwise blips, whose colour depends on whether
		// this cell is connected.
		final Image[] blips = isRoot ? BLIP_T_IMAGES
				: isConnected ? BLIP_IMAGES : BLIP_G_IMAGES;
		final int nblips = blips.length;
		int indexIn = Math.round((float) (nblips - 1) * frac) % nblips;
		if (indexIn < 0)
			indexIn = 0;
		int indexOut = Math.round((float) (nblips - 1) * (1 - frac)) % nblips;
		if (indexOut < 0)
			indexOut = 0;
		for (int c = 0; c < Dir.cardinals.length; ++c) {
			Dir d = Dir.cardinals[c];
			int ord = d.ordinal();
			final int xoff = Dir.cardinalOffs[c][0];
			final int yoff = Dir.cardinalOffs[c][1];
			if ((blipsIncoming & ord) != 0) {
				final float inp = (1.0f - frac) * cellWidth / 2f;
				final float x = sx + xoff * inp;
				final float y = sy + yoff * inp;
				Image blipImage = blips[indexIn];
				canvas.drawBitmap(blipImage.bitmap, x, y, cellPaint);
			}
			if ((blipsOutgoing & ord) != 0) {
				final float outp = frac * cellWidth / 2f;
				final float x = sx + xoff * outp;
				final float y = sy + yoff * outp;
				Image blipImage = blips[indexOut];
				canvas.drawBitmap(blipImage.bitmap, x, y, cellPaint);
			}
		}
	}

	/**
	 * This method is called to ask the cell to draw its active data blips. This
	 * happens in a separate pass, so that blips which are in transition from
	 * one cell to another don't get cropped by the drawing of the next cell.
	 * 
	 * @param canvas
	 *            Canvas to draw into.
	 * @param now
	 *            Current time in ms.
	 * @param frac
	 *            Fractional position of the data blips, if any, along whatever
	 *            connection leg they're on.
	 */
	private void drawTermData(Canvas canvas, long now, float frac) {
		// We don't check stateValid. Blips are always drawn.

		// If this cell is invisible or not connected, or there's no
		// blip, then nothing gets drawn.
		if (isBlind || !isConnected || blipsIncoming == 0)
			return;

		final int sx = cellLeft;
		final int sy = cellTop;

		cellPaint.setStyle(Paint.Style.STROKE);
		cellPaint.setStrokeWidth(1f);
		cellPaint.setColor(0xff00ff00);
		for (float y = cellHeight / 3f; y < cellHeight * 0.55f; y += 2f) {
			float l = cellWidth / 3f;
			float r = cellWidth / 3f * rng.nextFloat() + cellWidth / 3f;
			canvas.drawLine(sx + l, sy + y, sx + r, sy + y, cellPaint);
		}
	}

	// ******************************************************************** //
	// State Save/Restore.
	// ******************************************************************** //

	/**
	 * Save the game state of this cell, as part of saving the overall game
	 * state.
	 * 
	 * @return A Bundle containing the saved state.
	 */
	Bundle saveState() {
		Bundle map = new Bundle();

		// Save the aspects of the state which aren't part of the board
		// configuration (that gets re-created on reload).
		map.putString("connectedDirs", connectedDirs.toString());
		map.putFloat("currentAngle", rotateAngle);
		map.putInt("highlightPos", highlightPos);
		map.putBoolean("isConnected", isConnected);
		map.putBoolean("isFullyConnected", isFullyConnected);
		map.putBoolean("isBlind", isBlind);
		map.putBoolean("isRoot", isRoot);
		map.putBoolean("isLocked", isLocked);

		// Note: we don't save the focus state; focus save and restore
		// is done in BoardView.

		return map;
	}

	/**
	 * Restore the game state of this cell from the given Bundle, as part of
	 * restoring the overall game state.
	 * 
	 * @param map
	 *            A Bundle containing the saved state.
	 */
	void restoreState(Bundle map) {
		connectedDirs = Dir.valueOf(map.getString("connectedDirs"));
		rotateAngle = map.getFloat("currentAngle");
		highlightPos = map.getInt("highlightPos");
		isConnected = map.getBoolean("isConnected");
		isFullyConnected = map.getBoolean("isFullyConnected");
		isBlind = map.getBoolean("isBlind");
		isRoot = map.getBoolean("isRoot");
		isLocked = map.getBoolean("isLocked");

		// Phew! Time for a redraw... but we'll invalidate() at the
		// board level.
	}

	// ******************************************************************** //
	// Private Types.
	// ******************************************************************** //

	/**
	 * This enumeration defines the images, other than the cable images, which
	 * we use.
	 */
	private enum Image {
		NOTHING(R.drawable.nothing), EMPTY(R.drawable.empty), LOCKED(
				R.drawable.background_locked), BG(R.drawable.background), COMP1(
				R.drawable.computer1), COMP2(R.drawable.computer2), SERVER(
				R.drawable.server), SERVER1(R.drawable.server1), BLIP_T01(
				R.drawable.blip_t01), BLIP_T03(R.drawable.blip_t03), BLIP_T05(
				R.drawable.blip_t05), BLIP_T07(R.drawable.blip_t07), BLIP_T08(
				R.drawable.blip_t08), BLIP_T09(R.drawable.blip_t09), BLIP_T10(
				R.drawable.blip_t10), BLOB_14(R.drawable.blob_14), BLOB_15(
				R.drawable.blob_15), BLOB_16(R.drawable.blob_16), BLOB_17(
				R.drawable.blob_17), BLOB_18(R.drawable.blob_18), BLOB_19(
				R.drawable.blob_19), BLOB_20(R.drawable.blob_20), BLOBG_14(
				R.drawable.blob_g_14), BLOBG_15(R.drawable.blob_g_15), BLOBG_16(
				R.drawable.blob_g_16), BLOBG_17(R.drawable.blob_g_17), BLOBG_18(
				R.drawable.blob_g_18), BLOBG_19(R.drawable.blob_g_19), BLOBG_20(
				R.drawable.blob_g_20);

		private Image(int r) {
			resid = r;
		}

		public final int resid;
		public Bitmap bitmap = null;
	}

	// Images to show network data blips.
	private static final Image[] BLIP_IMAGES = { Image.BLOB_14, Image.BLOB_15,
			Image.BLOB_16, Image.BLOB_17, Image.BLOB_18, Image.BLOB_19,
			Image.BLOB_20, };

	// Images to show network data blips.
	private static final Image[] BLIP_G_IMAGES = { Image.BLOBG_14,
			Image.BLOBG_15, Image.BLOBG_16, Image.BLOBG_17, Image.BLOBG_18,
			Image.BLOBG_19, Image.BLOBG_20, };

	// Images to show network data blips arriving at a terminal.
	private static final Image[] BLIP_T_IMAGES = { Image.BLIP_T01,
			Image.BLIP_T03, Image.BLIP_T05, Image.BLIP_T07, Image.BLIP_T08,
			Image.BLIP_T09, Image.BLIP_T10, };

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "netscramble";

	// Default time taken to rotate a cell, in ms.
	private static final long ROTATE_DFLT_TIME = 250;

	// Time taken to display a highlight flash, in ms.
	private static final long HIGHLIGHT_TIME = 200;

	// Random number generator for the game. We use a Mersenne Twister,
	// which is a high-quality and fast implementation of java.util.Random.
	// private static final Random rng = new MTRandom();
	private static final SecureRandom rng = new SecureRandom();

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The cell's position in the board.
	private final int xindex, yindex;

	// Our neighbouring cells up, down, left and right. This changes
	// from game to game as each skill level has its own board size
	// and may or may not wrap. null if there is no neighbour in that
	// direction.
	private Cell nextU;
	private Cell nextD;
	private Cell nextL;
	private Cell nextR;

	// True iff this cell has the focus.
	private boolean haveFocus;

	// The directions in which this cell is isConnected. This is set up
	// at the start of each game.
	private Dir connectedDirs;

	// If we're currently rotating, the rotation target angle -- clockwise
	// positive, anti negative; the time in ms at which we started;
	// and the current angle in degrees. rotateDirection == 0 if not
	// rotating.
	private float rotateTarget = 0;
	private long rotateStart = 0;
	private float rotateAngle = 0f;

	// Duration of the current rotation, in ms.
	private long rotateTime = 250;

	// Status information for the highlight band across the cell.
	// This is used to draw a diagonal band of highlightPos flicking across the
	// cell, to highlight it when it is mis-clicked etc.
	// Flag whether there is a highlight currently showing; and if so,
	// the time in ms at which it started, and its current position across
	// the cell. The range of the latter is zero to cellWidth * 2.
	private boolean highlightOn = false;
	private long highlightStart = 0;
	private int highlightPos;

	// Blips drawn over the network to indicate the flow of data. A
	// blip moving in towards the centre of the cell is incoming; one
	// moving out is outgoing. An incoming blip becomes outgoing when
	// it hits the centre. Each connection direction can have one
	// blip on it, so we use bitmasks to track where blips are: each of
	// these values is just a bitmaps of which directions have a blip,
	// incoming or outgoing respectively.
	private int blipsIncoming = 0;
	private int blipsOutgoing = 0;

	// During a blip calculation pass, this value accumulates the data
	// blips that need to be passed on to other cells.
	private int blipsTransfer = 0;

	// True iff the cell is currently isConnected (directly or not)
	// to the server. This causes it to be displayed dark (not grey).
	private boolean isConnected;

	// True iff the cell is currently part of a fully connected network --
	// in other words, a solved puzzle. This may cause it to be displayed
	// differently; e.g. the server shows green LEDs.
	private boolean isFullyConnected;

	// True iff the cell is blind; ie. doesn't display its connections.
	// This is a difficulty factor.
	private boolean isBlind;

	// True iff this is the root cell of the network; ie. the server.
	private boolean isRoot;

	// True iff the cell has been isLocked by the user; this causes the
	// background to be highlighted.
	private boolean isLocked;

	// Cell's left X co-ordinate.
	private int cellLeft;

	// Cell's top Y co-ordinate.
	private int cellTop;

	// Cell's current width.
	private int cellWidth;

	// Cell's current height.
	private int cellHeight;

	// Painter used in onDraw().
	private Paint cellPaint;

	// True if the cell's rendered state is up to date.
	private boolean stateValid = false;

}
