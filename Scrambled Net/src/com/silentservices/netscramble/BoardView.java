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
 * © 2014 Michael Mueller <michael.mueller@silentservices.de>
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
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.Vector;

import org.hermit.android.core.SurfaceRunner;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.WindowManager;

import com.silentservices.netscramble.NetScramble.Sound;
import com.silentservices.netscramble.NetScramble.State;

/**
 * This implements the game board by laying out a grid of Cell objects.
 * 
 * Unlike the original knetwalk, we have to deal with a physical display whose
 * size can vary dramatically, but which we would like to fill; on the other
 * hand, it may be too small for a "full-sized" game, particularly bearing in
 * mind the minimum size a cell can be and still allow a finger to select it. We
 * therefore put a lot of work into figuring out how big the board should be.
 */
public class BoardView extends SurfaceRunner {

	// ******************************************************************** //
	// Configuration Constants.
	// ******************************************************************** //

	/**
	 * The minimum cell size in pixels.
	 */
	private static final int CELL_MIN = 28;

	/**
	 * The maximum cell size in pixels.
	 */
	private static final int CELL_MAX = 500;

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * Enumeration defining a game skill level. Each enum member also stores the
	 * configuration parameters for that skill level. We also introduce the
	 * wrinkle of blind tiles, to make an insane level:
	 */
	enum Skill {
		// Name id brch wrap blind
		NOVICE(R.string.skill_novice, R.id.skill_novice, 2, false, 9), NORMAL(
				R.string.skill_normal, R.id.skill_normal, 2, false, 9), EXPERT(
				R.string.skill_expert, R.id.skill_expert, 2, false, 9), MASTER(
				R.string.skill_master, R.id.skill_master, 3, true, 9), INSANE(
				R.string.skill_insane, R.id.skill_insane, 3, true, 3);

		private Skill(int lab, int i, int br, boolean w, int bd) {
			label = lab;
			id = i;
			branches = br;
			wrapped = w;
			blind = bd;
		}

		public final int label; // Res. ID of the label for this skill.
		public final int id; // Numeric ID for this skill level.
		public final int branches; // Max branches off each square; at least 2.
		public final boolean wrapped; // If true, network wraps around the
										// edges.
		public final int blind; // Squares with this many or more
								// connections are blind.
	}

	/**
	 * This enum defines the board sizes for the supported screen layouts.
	 * Traditional knetwalk has these board sizes: Novice: 5x5 = 25 tiles = 31%
	 * Normal: 7x7 = 49 tiles = 60% Expert: 9x9 = 81 tiles = 100% Master: 9x9 =
	 * 81 tiles = 100% wrapped
	 * 
	 * We have to deal with various screen sizes, and we want the cells to be
	 * big enough to touch. So, to set the board sizes, we choose either SMALL,
	 * MEDIUM, or LARGE based on the physical screen size. Then, sizes[skill][0]
	 * is the major grid size for that skill, and sizes[skill][1] is the minor
	 * grid size for that skill.
	 */
	enum Screen {
		SMALL(8, 6, 8, 6, 6, 6, 6, 4), // Like HVGA.
		WSMALL(9, 6, 9, 6, 5, 6, 5, 4), // Like HVGA.
		MEDIUM(11, 7, 11, 7, 9, 7, 5, 5), // VGA plus.
		WMEDIUM(12, 7, 10, 7, 8, 7, 6, 5), // Wide VGA plus.
		HUGE(17, 10, 15, 8, 11, 8, 7, 6); // WSVGA etc.

		Screen(int ml, int ms, int el, int es, int nl, int ns, int vl, int vs) {
			major = ml;
			minor = ms;
			sizes[Skill.INSANE.ordinal()][0] = ml;
			sizes[Skill.INSANE.ordinal()][1] = ms;
			sizes[Skill.MASTER.ordinal()][0] = ml;
			sizes[Skill.MASTER.ordinal()][1] = ms;
			sizes[Skill.EXPERT.ordinal()][0] = el;
			sizes[Skill.EXPERT.ordinal()][1] = es;
			sizes[Skill.NORMAL.ordinal()][0] = nl;
			sizes[Skill.NORMAL.ordinal()][1] = ns;
			sizes[Skill.NOVICE.ordinal()][0] = vl;
			sizes[Skill.NOVICE.ordinal()][1] = vs;
		}

		int getBoardWidth(Skill sk, int gw, int gh) {
			if (gw > gh)
				return sizes[sk.ordinal()][0];
			else
				return sizes[sk.ordinal()][1];
		}

		int getBoardHeight(Skill sk, int gw, int gh) {
			if (gw > gh)
				return sizes[sk.ordinal()][1];
			else
				return sizes[sk.ordinal()][0];
		}

		private final int major;
		private final int minor;
		private final int[][] sizes = new int[Skill.values().length][2];
	}

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Construct a board view.
	 * 
	 * @param context
	 *            The context we're running in.
	 * @param attrs
	 *            Our layout attributes.
	 */
	public BoardView(Context context, AttributeSet attrs) {
		super(context, attrs);

		try {
			NetScramble parent = (NetScramble) context;
			init(parent);
		} catch (ClassCastException e) {
			throw new IllegalStateException(
					"BoardView must be part of NetScramble", e);
		}
	}

	/**
	 * Construct a board view.
	 * 
	 * @param parent
	 *            The application context we're running in.
	 */
	public BoardView(NetScramble parent) {
		super(parent);
		init(parent);
	}

	/**
	 * Initialise this board view.
	 * 
	 * @param parent
	 *            The application context we're running in.
	 */
	private void init(NetScramble parent) {
		parentApp = parent;

		// Animation delay.
		setDelay(30);

		// Find out the device's screen dimensions and calculate the
		// size and shape of the cell matrix.
		findMatrix();

		// Create all the cells in the calculated board. In appSize()
		// we will take care of positioning them. Set the cell grid and root
		// so we have a valid state to save.
		Log.i(TAG, "Create board " + gridWidth + "x" + gridHeight);
		cellMatrix = new Cell[gridWidth][gridHeight];
		for (int y = 0; y < gridHeight; ++y) {
			for (int x = 0; x < gridWidth; ++x) {
				Cell cell = new Cell(parent, this, x, y);
				cellMatrix[x][y] = cell;
			}
		}
		rootCell = cellMatrix[0][0];

		// Create the connected flags and connecting cell connectingCells
		// (used in updateConnections()).
		isConnected = new boolean[gridWidth][gridHeight];
		connectingCells = new LinkedList<Cell>();

		// Set the initial focus on the root cell.
		focusedCell = null;
		setFocus(rootCell);
	}

	/**
	 * Find the size of board that can fit in the window.
	 */
	private void findMatrix() {
		WindowManager wm = (WindowManager) parentApp
				.getSystemService(Context.WINDOW_SERVICE);
		// Display disp = wm.getDefaultDisplay();
		// int width = disp.getWidth();
		// int height = disp.getHeight();
		DisplayMetrics display = this.getResources().getDisplayMetrics();

		int width = display.widthPixels;
		int height = display.heightPixels;
		int min = width < height ? width : height;
		int max = width > height ? width : height;
		float aspect = (float) max / (float) min;

		if (min <= 400) {
			if (aspect > 1.4f)
				screenConfig = Screen.WSMALL;
			else
				screenConfig = Screen.SMALL;
		} else if (min <= 500) {
			if (aspect > 1.5f)
				screenConfig = Screen.WMEDIUM;
			else
				screenConfig = Screen.MEDIUM;
		} else
			screenConfig = Screen.HUGE;

		if (width > height) {
			gridWidth = screenConfig.major;
			gridHeight = screenConfig.minor;
		} else {
			gridWidth = screenConfig.minor;
			gridHeight = screenConfig.major;
		}
		Log.v(TAG, "findMatrix: screen=" + width + "x" + height + " -> "
				+ screenConfig);
	}

	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	/**
	 * The application is starting. Perform any initial set-up prior to starting
	 * the application. We may not have a screen size yet, so this is not a good
	 * place to allocate resources which depend on that.
	 */
	@Override
	protected void appStart() {

	}

	/**
	 * Set the screen size. This is guaranteed to be called before animStart(),
	 * but perhaps not before appStart().
	 * 
	 * @param width
	 *            The new width of the surface.
	 * @param height
	 *            The new height of the surface.
	 * @param config
	 *            The pixel format of the surface.
	 */
	@Override
	protected void appSize(int width, int height, Bitmap.Config config) {
		// We usually get a zero-sized resize, which is useless;
		// ignore it.
		if (width < 1 || height < 1)
			return;

		// Create our backing bitmap.
		backingBitmap = getBitmap();
		backingCanvas = new Canvas(backingBitmap);

		// Calculate the cell size which makes the board fit. Make the cells
		// square.
		cellWidth = width / gridWidth;
		cellHeight = height / gridHeight;
		if (cellWidth < cellHeight)
			cellHeight = cellWidth;
		else if (cellHeight < cellWidth)
			cellWidth = cellHeight;

		// See if we have a usable size.
		if (cellWidth < CELL_MIN || cellHeight < CELL_MIN)
			throw new RuntimeException("Screen size is not playable.");
		if (cellWidth > CELL_MAX || cellHeight > CELL_MAX) {
			cellWidth = CELL_MAX;
			cellHeight = CELL_MAX;
		}

		// Set up the board configuration.
		Log.i(TAG, "Layout board " + gridWidth + "x" + gridHeight + ", "
				+ "cells " + cellWidth + "x" + cellHeight);

		// Center the board in the window.
		paddingX = (width - gridWidth * cellWidth) / 2;
		paddingY = (height - gridHeight * cellHeight) / 2;

		// Set the cell geometries and positions.
		for (int x = 0; x < gridWidth; ++x) {
			for (int y = 0; y < gridHeight; ++y) {
				int xPos = x * cellWidth + paddingX;
				int yPos = y * cellHeight + paddingY;
				cellMatrix[x][y].setGeometry(xPos, yPos, cellWidth, cellHeight);
			}
		}

		// Load all the pixmaps for the game tiles etc.
		Cell.initPixmaps(parentApp.getResources(), cellWidth, cellHeight,
				config);
	}

	/**
	 * We are starting the animation loop. The screen size is known.
	 * 
	 * <p>
	 * doUpdate() and doDraw() may be called from this point on.
	 */
	@Override
	protected void animStart() {

	}

	/**
	 * We are stopping the animation loop, for example to pause the app.
	 * 
	 * <p>
	 * doUpdate() and doDraw() will not be called from this point on.
	 */
	@Override
	protected void animStop() {

	}

	/**
	 * The application is closing down. Clean up any resources.
	 */
	@Override
	protected void appStop() {

	}

	// ******************************************************************** //
	// Board Setup.
	// ******************************************************************** //

	/**
	 * Enable or disable the network animation.
	 * 
	 * @param enable
	 *            New network animation enablement state.
	 */
	void setAnimEnable(boolean enable) {
		drawBlips = enable;
	}

	/**
	 * Set up the board for a new game.
	 * 
	 * @param sk
	 *            Skill level for the game; set the board up accordingly.
	 */
	public void setupBoard(Skill sk) {
		autosolveStop();
		gameSkill = sk;

		// Reset the board for this game.
		resetBoard(sk);

		// Require at least 85% of the cells active.
		int minCells = (int) (boardWidth * boardHeight * 0.85);

		// Loop doing board setup until we get a valid board.
		int tries = 0, cells = 0;
		for (tries = 0; cells < minCells && tries < 10; ++tries)
			cells = createNet(sk);
		Log.i(TAG, "Created net in " + tries + " tries with " + cells
				+ " cells (min " + minCells + ")");

		// Now, save the "solved" state of the board.
		solvedState = new Bundle();
		saveBoard(solvedState);

		// Jumble the board. Also, if we're in blind mode, tell the
		// appropriate cells to go blind.
		for (int x = boardStartX; x < boardEndX; x++) {
			for (int y = boardStartY; y < boardEndY; y++) {
				cellMatrix[x][y].rotate((rng.nextInt(4) - 2) * 90);
				if (cellMatrix[x][y].numDirs() >= sk.blind)
					cellMatrix[x][y].setBlind(true);
			}
		}

		// Figure out the active connections.
		updateConnections();

		// Invalidate all the cells.
		for (int x = 0; x < gridWidth; x++)
			for (int y = 0; y < gridHeight; y++)
				cellMatrix[x][y].invalidate();
	}

	/**
	 * Reset the board for a given skill level.
	 * 
	 * @param sk
	 *            Skill level for the game; set the board up accordingly.
	 */
	private void resetBoard(Skill sk) {
		// Save the width and height of the playing board for this skill
		// level, and the board placement within the overall cell grid.
		boardWidth = screenConfig.getBoardWidth(sk, gridWidth, gridHeight);
		boardHeight = screenConfig.getBoardHeight(sk, gridWidth, gridHeight);
		boardStartX = (gridWidth - boardWidth) / 2;
		boardEndX = boardStartX + boardWidth;
		boardStartY = (gridHeight - boardHeight) / 2;
		boardEndY = boardStartY + boardHeight;

		// Reset the cells. If we're wrapped, set the surrounding cells
		// to None; else Free, to show that there's no wraparound.
		Log.i(TAG, "Reset board " + gridWidth + "x" + gridHeight);
		boolean wrap = gameSkill.wrapped;
		Cell u, d, l, r;
		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				cellMatrix[x][y].reset(wrap ? Cell.Dir.NONE : Cell.Dir.FREE);

				// Re-calculate who this cell's neighbours are.
				u = d = l = r = null;
				if (wrap || y > boardStartY)
					u = cellMatrix[x][decr(y, boardStartY, boardEndY)];
				if (wrap || y < boardEndY - 1)
					d = cellMatrix[x][incr(y, boardStartY, boardEndY)];
				if (wrap || x > boardStartX)
					l = cellMatrix[decr(x, boardStartX, boardEndX)][y];
				if (wrap || x < boardEndX - 1)
					r = cellMatrix[incr(x, boardStartX, boardEndX)][y];
				cellMatrix[x][y].setNeighbours(u, d, l, r);
			}
		}
	}

	/**
	 * Get the current playing area width. This varies with the skill level.
	 * 
	 * @return Playing area width in tiles.
	 */
	int getBoardWidth() {
		return boardWidth;
	}

	/**
	 * Get the current playing area height. This varies with the skill level.
	 * 
	 * @return Playing area height in tiles.
	 */
	int getBoardHeight() {
		return boardHeight;
	}

	/**
	 * Create a network layout. This function may be called multiple times after
	 * resetBoard(), to get a network with enough cells.
	 * 
	 * @param sk
	 *            Skill level for the game; create the network accordingly.
	 * @return The number of cells used in the layout.
	 */
	private int createNet(Skill sk) {
		Log.i(TAG, "Create net " + boardStartX + "-" + boardEndX + ", "
				+ boardStartY + "-" + boardEndY);

		// Reset the cells' directions, and reset the root cell.
		for (int x = boardStartX; x < boardEndX; x++) {
			for (int y = boardStartY; y < boardEndY; y++) {
				cellMatrix[x][y].setDirs(Cell.Dir.FREE);
				cellMatrix[x][y].setRoot(false);
			}
		}

		// Set the rootCell cell (the server) to a random cell.
		int rootX = rng.nextInt(boardWidth) + boardStartX;
		int rootY = rng.nextInt(boardHeight) + boardStartY;
		rootCell = cellMatrix[rootX][rootY];
		rootCell.setConnected(true);
		rootCell.setRoot(true);
		// Log.i(TAG, "Root cell " + rootCell.x() + "," + rootCell.y() + " (" +
		// rootX + "," + rootY + ")");
		setFocus(rootCell);

		// Set up the connectingCells of cells awaiting connection. Start
		// by adding the root cell.
		Vector<Cell> list = new Vector<Cell>();
		list.add(rootCell);
		if (rng.nextBoolean())
			addRandomDir(list);

		// Loop while there are still cells to be connected, connecting
		// them in random directions.
		while (!list.isEmpty()) {
			// Randomly do the first cell, or defer it and do the next one.
			// This prevents unduly long, straight branches.
			if (rng.nextBoolean()) {
				// Add a random direction from this cell.
				addRandomDir(list);

				// 50% of the time, add a second direction, if we can
				// find one.
				if (rng.nextBoolean())
					addRandomDir(list);

				// A third pass makes networks more complex, but also
				// introduces 4-way crosses.
				if (sk.branches >= 3 && rng.nextInt(3) == 0)
					addRandomDir(list);
			} else
				list.add(list.firstElement());

			// Pop the first element off the connectingCells.
			list.remove(0);
		}

		// Count the number of connected cells in this board.
		int cells = 0;
		for (int x = boardStartX; x < boardEndX; x++)
			for (int y = boardStartY; y < boardEndY; y++)
				if (cellMatrix[x][y].dirs() != Cell.Dir.FREE)
					++cells;

		Log.i(TAG, "Created net with " + cells + " cells");
		return cells;
	}

	/**
	 * Add a connection in a random direction from the first cell in the given
	 * cell connectingCells. We enumerate the free adjacent cells around the
	 * starting cell, then pick one to connect to at random. If there is no free
	 * adjacent cell, we do nothing.
	 * 
	 * If we connect to a cell, it is added to the passed-in connectingCells.
	 * 
	 * @param list
	 *            Current list of cells awaiting connection.
	 */
	private void addRandomDir(Vector<Cell> list) {
		// Start with the first cell in the cell connectingCells.
		Cell cell = list.firstElement();

		// List the adjacent cells which are free.
		EnumMap<Cell.Dir, Cell> freecells = new EnumMap<Cell.Dir, Cell>(
				Cell.Dir.class);
		for (Cell.Dir d : Cell.Dir.cardinals) {
			Cell ucell = cell.next(d);
			if (ucell != null && ucell.dirs() == Cell.Dir.FREE)
				freecells.put(d, ucell);
		}

		if (freecells.isEmpty()) {
			// Log.d(TAG, "addRandomDir: no free adjacents");
			return;
		}

		// Pick one of the free adjacents at random.
		Object[] keys = freecells.keySet().toArray();
		Cell.Dir key = (Cell.Dir) keys[rng.nextInt(keys.length)];
		Cell dest = freecells.get(key);

		// Make a link to that cell, and a corresponding link back.
		cell.addDir(key);
		dest.addDir(key.reverse);

		// Add the new cell to the outstanding connectingCells.
		list.add(dest);
		// Log.d(TAG, "addRandomDir: connected to " + dest.x() + "," +
		// dest.y());
	}

	// ******************************************************************** //
	// Board Logic.
	// ******************************************************************** //

	/**
	 * Scan the board to see which cells are connected to the server. Update the
	 * state of every cell accordingly. This function is called each time a cell
	 * is rotated, to re-compute the connectedness of every cell.
	 * 
	 * @return true iff one or more cells have been connected that previously
	 *         weren't.
	 */
	private synchronized boolean updateConnections() {
		// Reset the array of connected flags per cell.
		for (int x = 0; x < gridWidth; x++)
			for (int y = 0; y < gridHeight; y++)
				isConnected[x][y] = false;

		// Clear the list of cells which are connected but
		// haven't had their onward connections checked yet.
		connectingCells.clear();

		// If the root cell is rotated, then it's not connected to
		// anything -- no-one is connected. Otherwise, flag the root
		// cell as connected and add it to the connectingCells.
		if (!rootCell.isRotated()) {
			isConnected[rootCell.x()][rootCell.y()] = true;
			connectingCells.add(rootCell);
		}

		// While there are still cells to investigate, check them for
		// connections that we haven't flagged yet, and add those cells
		// to the connectingCells.
		while (!connectingCells.isEmpty()) {
			Cell cell = connectingCells.remove();

			for (Cell.Dir d : Cell.Dir.cardinals)
				if (hasNewConnection(cell, d, isConnected))
					connectingCells.add(cell.next(d));
		}

		// Finally, scan the connection flags. Set every cell's connected
		// status accordingly. Count connected cells, and cells that are
		// connected but weren't previously.
		int newConnections = 0;
		for (int x = 0; x < gridWidth; x++) {
			for (int y = 0; y < gridHeight; y++) {
				if (isConnected[x][y]) {
					if (!cellMatrix[x][y].isConnected())
						++newConnections;
				}
				cellMatrix[x][y].setConnected(isConnected[x][y]);
			}
		}

		// Log.d(TAG, "updateConnections: " + connections +
		// " connected (" + newConnections + " new)");

		// Tell the caller whether we got a new one.
		return newConnections != 0;
	}

	/**
	 * Determine whether we have a connection from the given cell in the given
	 * direction which hasn't already been logged in got[][].
	 * 
	 * @param cell
	 *            Starting cell.
	 * @param dir
	 *            Direction to look in.
	 * @param got
	 *            Array of flags showing which cells we have already found
	 *            connections for. If we find a new connection, we will set the
	 *            flag for it in here.
	 * @return true iff we found a new connection in the given direction.
	 */
	private boolean hasNewConnection(Cell cell, Cell.Dir dir, boolean got[][]) {
		// Find the cell we're going to, if any.
		Cell other = cell.next(dir);
		Cell.Dir otherdir = dir.reverse;

		// If there's no cell there, then there's no connection. If we
		// have already marked it connected, we're done.
		if (other == null || got[other.x()][other.y()])
			return false;

		// See if there's an actual connection. If either cell is rotated,
		// there's no connection.
		if (!cell.hasConnection(dir) || !other.hasConnection(otherdir))
			return false;

		// OK, there's a connection, and it's new. Mark it.
		got[other.x()][other.y()] = true;
		return true;
	}

	/**
	 * Determine whether the board is currently in a solved state -- i.e. all
	 * terminals are connected to the server.
	 * 
	 * Note that in some layouts, it is possible to connect all the terminals
	 * without using all the cable sections. Since the game intro asks the user
	 * to connect all the terminals, which makes sense, we look for unconnected
	 * terminals specifically.
	 * 
	 * NOTE: We assume that updateConnections() has been called to set the
	 * connected states of all cells correctly for the current board state.
	 * 
	 * @return true iff the board is currently in a solved state -- ie. every
	 *         terminal cell is connected to the server.
	 */
	synchronized boolean isSolved() {
		// Scan the board; any non-connected non-empty cell means
		// we're not done yet.
		for (int x = boardStartX; x < boardEndX; x++) {
			for (int y = boardStartY; y < boardEndY; y++) {
				Cell cell = cellMatrix[x][y];

				// If there's an unconnected terminal, we're not solved.
				if (cell.numDirs() == 1 && !cell.isConnected())
					return false;
			}
		}

		return true;
	}

	/**
	 * Count the number of unconnected cells in the board.
	 * 
	 * Note that in some layouts (particularly in Expert mode), it is possible
	 * to connect all the terminals without using all the cable sections, so the
	 * answer may be non-0 on a solved board.
	 * 
	 * NOTE: We assume that updateConnections() has been called to set the
	 * connected states of all cells correctly for the current board state.
	 * 
	 * @return The number of unconnected cells in the board.
	 */
	int unconnectedCells() {
		int unused = 0;

		for (int x = boardStartX; x < boardEndX; x++) {
			for (int y = boardStartY; y < boardEndY; y++) {
				Cell cell = cellMatrix[x][y];
				if (cell.dirs() != Cell.Dir.FREE && !cell.isConnected())
					++unused;
			}
		}

		return unused;
	}

	// ******************************************************************** //
	// Client Methods.
	// ******************************************************************** //

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
	 */
	@Override
	protected void doUpdate(long now) {
		// See if we have programmed moves to execute. If so, see if
		// it's time for the next one.
		if (programmedMoves != null && now - lastProgMove > SOLVE_STEP_TIME) {
			if (programmedMoves.isEmpty()) {
				// Since the last move has completed, we're now finished.
				autosolveStop();
			} else {
				// Get the next move and execute it.
				int[] move = programmedMoves.removeFirst();
				Cell mc = cellMatrix[move[0]][move[1]];
				int dirn = move[2];

				// If the cell isn't focused, focus it, and that's our move.
				// If the cell is locked, unlock it, and that's our move.
				// Otherwise do the actual move and update the connection state.
				if (mc != focusedCell) {
					setFocus(mc);
					programmedMoves.addFirst(move);
				} else if (mc.isLocked()) {
					mc.setLocked(false);
					programmedMoves.addFirst(move);
				} else {
					// Make the cell's content visible. Do the move.
					mc.setBlind(false);
					mc.rotate(dirn, SOLVE_ROTATE_TIME);
					updateConnections();
				}

				lastProgMove = now;
			}
		}

		// Update all the cells. Flag if any cell changed its
		// connection state.
		Cell changedCell = null;
		for (int x = 0; x < gridWidth; ++x)
			for (int y = 0; y < gridHeight; ++y)
				if (cellMatrix[x][y].doUpdate(now))
					changedCell = cellMatrix[x][y];

		// Update all the data blips.
		if (drawBlips) {
			if (now - blipsLastAdvance >= BLIPS_TIME) {
				for (int x = 0; x < gridWidth; ++x)
					for (int y = 0; y < gridHeight; ++y)
						cellMatrix[x][y].advanceBlips(blipCount);
				++blipCount;
				for (int x = 0; x < gridWidth; ++x)
					for (int y = 0; y < gridHeight; ++y)
						cellMatrix[x][y].transferBlips();
				blipsLastAdvance += BLIPS_TIME;
				if (blipsLastAdvance < now)
					blipsLastAdvance = now;
			}
		}

		// If the connection state changed, update the network.
		if (changedCell != null) {
			if (updateConnections())
				parentApp.postSound(Sound.CONNECT);

			// If we're done, report it.
			if (isSolved()) {
				// Un-blind all cells.
				for (int x = boardStartX; x < boardEndX; x++)
					for (int y = boardStartY; y < boardEndY; y++)
						cellMatrix[x][y].setBlind(false);

				blink(changedCell);
				parentApp.postState(State.SOLVED);
				parentApp.postSound(Sound.WIN);
			}
		}
	}

	/**
	 * Draw the current frame of the application.
	 * 
	 * <p>
	 * Applications must override this, and are expected to draw the entire
	 * screen into the provided canvas.
	 * 
	 * <p>
	 * This method will always be called after a call to doUpdate(), and also
	 * when the screen needs to be re-drawn.
	 * 
	 * @param canvas
	 *            The Canvas to draw into.
	 * @param now
	 *            Current time in ms. Will be the same as that passed to
	 *            doUpdate(), if there was a preceding call to doUpdate().
	 */
	@Override
	protected void doDraw(Canvas canvas, long now) {
		// Draw all the cells into the backing bitmap. Only the
		// dirty cells will redraw themselves.
		for (int x = 0; x < gridWidth; ++x)
			for (int y = 0; y < gridHeight; ++y)
				cellMatrix[x][y].doDraw(backingCanvas, now);

		// Now push the backing bitmap to the screen.
		canvas.drawBitmap(backingBitmap, 0, 0, null);

		// Draw the data blips in a separate pass so they can overlap
		// adjacent cells without getting overdrawn. We draw directly
		// to the screen.
		if (drawBlips) {
			float frac = (float) (now - blipsLastAdvance) / (float) BLIPS_TIME;
			for (int x = 0; x < gridWidth; ++x)
				for (int y = 0; y < gridHeight; ++y)
					cellMatrix[x][y].doDrawBlips(canvas, now, frac);
		}
	}

	// ******************************************************************** //
	// Input Handling.
	// ******************************************************************** //

	/**
	 * Handle key input.
	 * 
	 * @param keyCode
	 *            The key code.
	 * @param event
	 *            The KeyEvent object that defines the button action.
	 * @return True if the event was handled, false otherwise.
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// Take care of calling onBackPressed() on earlier versions of
		// the platform where it doesn't exist.
		// TODO: delete this code when no longer supporting pre-2.0.
		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR
				&& keyCode == KeyEvent.KEYCODE_BACK
				&& event.getRepeatCount() == 0) {
			parentApp.onBackPressed();
			return true;
		}

		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			// DPAD_CENTER is special: handle like a screen press, and check
			// for a long press.
			pressDown();
			return true;
		case KeyEvent.KEYCODE_ENTER:
			if (programmedMoves == null)
				cellRotate(focusedCell, 1);
			return true;
		case KeyEvent.KEYCODE_Z:
		case KeyEvent.KEYCODE_N:
		case KeyEvent.KEYCODE_4:
			if (programmedMoves == null)
				cellRotate(focusedCell, -1);
			return true;
		case KeyEvent.KEYCODE_X:
		case KeyEvent.KEYCODE_M:
		case KeyEvent.KEYCODE_6:
			if (programmedMoves == null)
				cellRotate(focusedCell, 1);
			return true;
		case KeyEvent.KEYCODE_SPACE:
		case KeyEvent.KEYCODE_0:
			if (programmedMoves == null)
				cellToggleLock(focusedCell);
			return true;
		case KeyEvent.KEYCODE_P:
		case KeyEvent.KEYCODE_9:
			pauseGame();
			return true;

		case KeyEvent.KEYCODE_DPAD_UP:
			if (programmedMoves == null)
				moveFocus(Cell.Dir.U___, 0, -1);
			return true;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (programmedMoves == null)
				moveFocus(Cell.Dir._R__, 1, 0);
			return true;
		case KeyEvent.KEYCODE_DPAD_DOWN:
			if (programmedMoves == null)
				moveFocus(Cell.Dir.__D_, 0, 1);
			return true;
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (programmedMoves == null)
				moveFocus(Cell.Dir.___L, -1, 0);
			return true;
		}

		return false;
	}

	/**
	 * Handle key input.
	 * 
	 * @param keyCode
	 *            The key code.
	 * @param event
	 *            The KeyEvent object that defines the button action.
	 * @return True if the event was handled, false otherwise.
	 */
	@Override
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		switch (keyCode) {
		case KeyEvent.KEYCODE_DPAD_CENTER:
			// Special handling for DPAD_CENTER: cancel long press handling.
			pressUp();
			return true;
		}

		return false;
	}

	/**
	 * Handle trackball motion events.
	 * 
	 * @param event
	 *            The motion event.
	 * @return True if the event was handled, false otherwise.
	 */
	@Override
	public boolean onTrackballEvent(MotionEvent event) {
		// Actually, just let these come through as D-pad events.
		return false;
	}

	/**
	 * Handle MotionEvent events.
	 * 
	 * @param event
	 *            The motion event.
	 * @return True if the event was handled, false otherwise.
	 */
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (event.getAction() == MotionEvent.ACTION_DOWN) {
			// Focus on the pressed cell.
			pressedCell = findCell(event.getX(), event.getY());
			if (pressedCell != null) {
				if (programmedMoves == null)
					setFocus(pressedCell);
				pressDown();
			}
		} else if (event.getAction() == MotionEvent.ACTION_UP) {
			if (pressedCell != null) {
				pressedCell = null;
				pressUp();
			}
		}

		return true;
	}

	/**
	 * Find the cell corresponding to a screen location.
	 * 
	 * @param x
	 *            Location X.
	 * @param y
	 *            Location Y.
	 * @return The cell at x,y; null if none.
	 */
	private Cell findCell(float x, float y) {
		// Focus on the pressed cell.
		int cx = (int) ((x - paddingX) / cellWidth);
		int cy = (int) ((y - paddingY) / cellHeight);
		if (cx < 0 || cx >= gridWidth || cy < 0 || cy >= gridHeight)
			return null;
		return cellMatrix[cx][cy];
	}

	/**
	 * Handle a screen or centre-button press.
	 */
	private void pressDown() {
		// Get ready to detect a long press.
		longPressed = false;
		longPressHandler.postDelayed(longPress, LONG_PRESS);
	}

	/**
	 * Handle a screen or centre-button release. If we didn't get a long press,
	 * handle like a click.
	 */
	private void pressUp() {
		if (!longPressed) {
			// Cancel the long press handler.
			longPressHandler.removeCallbacks(longPress);

			// If we got here, rotate the cell -- except user input is ignored
			// while executing programmed moves.
			if (programmedMoves == null)
				cellRotate(focusedCell, 1);
		}
	}

	/**
	 * Handler for a screen or centre-button long press.
	 */
	private Runnable longPress = new Runnable() {
		@Override
		public void run() {
			longPressed = true;
			if (programmedMoves == null)
				cellToggleLock(focusedCell);
		}
	};

	/**
	 * Move the cell focus in the given direction.
	 * 
	 * Note that this moves the variable focusedCell; we do our own focus,
	 * rather than using system focus, as there seems to be no way to make that
	 * wrap on a dynamic layout.
	 * 
	 * @param dir
	 *            The direction to move in.
	 * @param dx
	 *            X-delta of that direction (for convenience).
	 * @param dy
	 *            Y-delta of that direction (for convenience).
	 */
	private void moveFocus(Cell.Dir dir, int dx, int dy) {
		if (focusedCell == null)
			return;

		// Try using the cell's idea of it's neighbour.
		Cell goCell = focusedCell.next(dir);

		// Otherwise wrap around the board.
		if (goCell == null) {
			int nx = (focusedCell.x() + dx + gridWidth) % gridWidth;
			int ny = (focusedCell.y() + dy + gridHeight) % gridHeight;
			goCell = cellMatrix[nx][ny];
		}

		setFocus(goCell);
	}

	// ******************************************************************** //
	// Cell Actions.
	// ******************************************************************** //

	/**
	 * Set the focused cell to the given cell.
	 * 
	 * Note that this moves the variable focusedCell; we do our own focus,
	 * rather than using system focus, as there seems to be no way to make that
	 * wrap on a dynamic layout. (focusSearchInDescendants doesn't get called.)
	 * 
	 * @param cell
	 *            The cell; null to clear focus.
	 */
	private void setFocus(Cell cell) {
		if (focusedCell != null)
			focusedCell.setFocused(false);
		focusedCell = cell;
		if (focusedCell != null)
			focusedCell.setFocused(true);
	}

	/**
	 * The given cell has been told to rotate.
	 * 
	 * @param cell
	 *            The cell.
	 * @param dirn
	 *            Direction: -1 for left, 1 for right.
	 */
	void cellRotate(Cell cell, int dirn) {
		// See if the cell is empty or locked; give the user some negative
		// feedback if so.
		Cell.Dir d = cell.dirs();
		if (d == Cell.Dir.FREE || d == Cell.Dir.NONE || cell.isLocked()) {
			parentApp.postSound(Sound.CLICK);
			blink(cell);
			return;
		}

		// Give the user a click. Set up an animation to do the rotation.
		parentApp.postSound(Sound.TURN);
		cell.rotate(dirn * 90);

		// This cell is no longer connected. Update the connection state.
		updateConnections();

		// Tell the parent we clicked this cell.
		parentApp.cellClicked(cell);
	}

	/**
	 * Toggle the locked state of the given cell.
	 * 
	 * @param cell
	 *            The cell to toggle.
	 */
	void cellToggleLock(Cell cell) {
		// See if the cell is empty; give the user some negative
		// feedback if so.
		Cell.Dir d = cell.dirs();
		if (d == Cell.Dir.FREE || d == Cell.Dir.NONE) {
			parentApp.postSound(Sound.CLICK);
			blink(cell);
			return;
		}

		cell.setLocked(!cell.isLocked());
		parentApp.postSound(Sound.POP);
	}

	/**
	 * Pause the game.
	 */
	private void pauseGame() {
		parentApp.postState(State.PAUSED);
	}

	/**
	 * Blink the given cell, to indicate a mis-click etc.
	 * 
	 * @param cell
	 *            The cell to blink.
	 */
	private void blink(Cell cell) {
		cell.doHighlight();
	}

	/**
	 * Set the board to display the game as solved.
	 */
	void setSolved() {
		// Display the fully-connected version of the server.
		rootCell.setSolved(true);
	}

	// ******************************************************************** //
	// Autosolver.
	// ******************************************************************** //

	/**
	 * Auto-solve the puzzle, by generating a list of programmed moves which
	 * will set each cell to the position it was in when the network was
	 * created.
	 * 
	 * We generate the moves list in breadth-first order. This is harder to do,
	 * but looks nicer.
	 */
	void autosolve() {
		// If we're already solving, just toggle the state.
		if (programmedMoves != null) {
			autosolveStop();
			return;
		}

		Bundle solution = solvedState;
		if (solution == null)
			return;

		// Read the solved state, accounting for device rotations etc.
		GameState state = new GameState(gridWidth, gridHeight, gameSkill);
		if (!restoreBoard(solution, state))
			return;

		// Create the programmed move list.
		programmedMoves = new LinkedList<int[]>();

		// Clear the list of cells which are connected but
		// haven't had their onward connections checked yet.
		connectingCells.clear();

		// Reset the array of solved flags per cell.
		for (int x = 0; x < gridWidth; x++)
			for (int y = 0; y < gridHeight; y++)
				isConnected[x][y] = false;

		// Set the root cell up to be solved first.
		connectingCells.add(state.root);
		isConnected[state.root.x()][state.root.y()] = true;

		// While there are still cells to investigate, solve them, check
		// them for connections that we haven't flagged yet, and add those
		// cells to the connectingCells.
		while (!connectingCells.isEmpty()) {
			Cell cell = connectingCells.removeFirst();
			solveCell(cell, programmedMoves);

			for (Cell.Dir d : Cell.Dir.cardinals) {
				if (cell.hasConnection(d)) {
					Cell next = cell.next(d);
					if (next != null && !isConnected[next.x()][next.y()]) {
						connectingCells.addLast(next);
						isConnected[next.x()][next.y()] = true;
					}
				}
			}
		}

		lastProgMove = 0;
		parentApp.selectAutosolveMode(true);
	}

	/**
	 * Stop the autosolver.
	 */
	void autosolveStop() {
		programmedMoves = null;
		lastProgMove = 0;
		parentApp.selectAutosolveMode(false);
	}

	/**
	 * Solve the given cell. This doesn't actually do anything, except add a
	 * move to the given moves list to put the cell into the solved state.
	 * 
	 * @param sc
	 *            The solved version of the cell.
	 * @param moves
	 *            List of moves that we're building.
	 */
	private void solveCell(Cell sc, LinkedList<int[]> moves) {
		Cell mc = cellMatrix[sc.x()][sc.y()];
		Cell.Dir sd = sc.dirs();
		Cell.Dir md = mc.dirs();
		if (sc.dirs() != md) {
			int[] move;
			if (mc.rotatedDirs(90) == sd) {
				move = new int[] { mc.x(), mc.y(), 90 };
				moves.add(move);
			} else if (mc.rotatedDirs(-90) == sd) {
				move = new int[] { mc.x(), mc.y(), -90 };
				moves.add(move);
			} else if (mc.rotatedDirs(180) == sd) {
				int rot = rng.nextBoolean() ? 90 : -90;
				move = new int[] { mc.x(), mc.y(), rot };
				moves.add(move);
				move = new int[] { mc.x(), mc.y(), rot };
				moves.add(move);
			}
		}
	}

	// ******************************************************************** //
	// State Save/Restore.
	// ******************************************************************** //

	/**
	 * Save game state so that the user does not lose anything if the game
	 * process is killed while we are in the background.
	 * 
	 * @param outState
	 *            A Bundle in which to place any state information we wish to
	 *            save.
	 */
	protected void saveState(Bundle outState) {
		// Save the game state of the board.
		saveBoard(outState);

		// Also save the solved state, if any.
		if (solvedState != null)
			outState.putBundle("solvedState", solvedState);
	}

	/**
	 * Save game state so that the user does not lose anything if the game
	 * process is killed while we are in the background.
	 * 
	 * @param outState
	 *            A Bundle in which to place any state information we wish to
	 *            save.
	 */
	private void saveBoard(Bundle outState) {
		outState.putInt("gridWidth", gridWidth);
		outState.putInt("gridHeight", gridHeight);
		outState.putInt("rootX", rootCell.x());
		outState.putInt("rootY", rootCell.y());
		outState.putInt("focusX", focusedCell.x());
		outState.putInt("focusY", focusedCell.y());

		// Save the states of all the cells which are in use.
		for (int x = 0; x < gridWidth; ++x) {
			for (int y = 0; y < gridHeight; ++y) {
				String key = "cell " + x + "," + y;
				outState.putBundle(key, cellMatrix[x][y].saveState());
			}
		}
	}

	/**
	 * Restore our game state from the given Bundle.
	 * 
	 * @param map
	 *            A Bundle containing the saved state.
	 * @param skill
	 *            Skill level of the saved game.
	 * @return true if the state was restored OK; false if the saved state was
	 *         incompatible with the current configuration.
	 */
	boolean restoreState(Bundle map, Skill skill) {
		// Restore the game state of the board.
		gameSkill = skill;
		resetBoard(skill);

		// Restore the actual game state.
		GameState state = new GameState(cellMatrix);
		boolean ok = restoreBoard(map, state);
		rootCell = state.root;
		setFocus(state.focus);

		// Also restore the solved state, if any.
		if (ok && map.containsKey("solvedState")) {
			solvedState = map.getBundle("solvedState");
			ok = solvedState != null;
		}

		return ok;
	}

	/**
	 * Restore our game state from the given Bundle.
	 * 
	 * @param map
	 *            A Bundle containing the saved state.
	 * @param state
	 *            Record to place the restored state in.
	 * @return true if the state was restored OK; false if the saved state was
	 *         incompatible with the current configuration.
	 */
	private boolean restoreBoard(Bundle map, GameState state) {
		// Check that the saved board size is compatible with what we
		// have now. If it is identical, then do a straight restore; if
		// it's rotated, then restore and rotate.
		int sgw = map.getInt("gridWidth");
		int sgh = map.getInt("gridHeight");
		if (sgw == gridWidth && sgh == gridHeight)
			return restoreNormal(map, state);
		else if (sgw == gridHeight && sgh == gridWidth) {
			if (gridWidth > gridHeight)
				return restoreRotLeft(map, state);
			else
				return restoreRotRight(map, state);
		} else
			return false;
	}

	/**
	 * Restore our game state from the given Bundle.
	 * 
	 * @param map
	 *            A Bundle containing the saved state.
	 * @param state
	 *            Record to place the restored state in.
	 * @return true if the state was restored OK; false if the saved state was
	 *         incompatible with the current configuration.
	 */
	private boolean restoreNormal(Bundle map, GameState state) {
		// Set up the root cell and focused cell.
		int rx = map.getInt("rootX");
		int ry = map.getInt("rootY");
		state.root = state.matrix[rx][ry];
		int fx = map.getInt("focusX");
		int fy = map.getInt("focusY");
		state.focus = state.matrix[fx][fy];

		// Restore the states of all the cells which are in use.
		for (int x = 0; x < gridWidth; ++x) {
			for (int y = 0; y < gridHeight; ++y) {
				String key = "cell " + x + "," + y;
				state.matrix[x][y].restoreState(map.getBundle(key));
			}
		}

		return true;
	}

	/**
	 * Restore our game state from the given Bundle, rotating the board left as
	 * we do so.
	 * 
	 * @param map
	 *            A Bundle containing the saved state.
	 * @param state
	 *            Record to place the restored state in.
	 * @return true if the state was restored OK; false if the saved state was
	 *         incompatible with the current configuration.
	 */
	private boolean restoreRotLeft(Bundle map, GameState state) {
		// Set up the root cell. Flip the co-ordinates.
		int rx = map.getInt("rootX");
		int ry = map.getInt("rootY");
		state.root = state.matrix[ry][gridHeight - rx - 1];
		int fx = map.getInt("focusX");
		int fy = map.getInt("focusY");
		state.focus = state.matrix[fy][gridHeight - fx - 1];

		// Restore the states of all the cells which are in use.
		for (int y = 0; y < gridHeight; ++y) {
			for (int x = 0; x < gridWidth; ++x) {
				if (x >= state.matrix.length || y >= state.matrix[x].length)
					return false;
				String key = "cell " + y + "," + x;
				Bundle cmap = map.getBundle(key);
				if (cmap == null)
					return false;
				state.matrix[x][gridHeight - y - 1].restoreState(cmap);
				state.matrix[x][gridHeight - y - 1].rotateImmediate(-90);
			}
		}

		return true;
	}

	/**
	 * Restore our game state from the given Bundle, rotating the board right as
	 * we do so.
	 * 
	 * @param map
	 *            A Bundle containing the saved state.
	 * @param state
	 *            Record to place the restored state in.
	 * @return true if the state was restored OK; false if the saved state was
	 *         incompatible with the current configuration.
	 */
	private boolean restoreRotRight(Bundle map, GameState state) {
		// Set up the root cell. Flip the co-ordinates.
		int rx = map.getInt("rootX");
		int ry = map.getInt("rootY");
		state.root = state.matrix[gridWidth - ry - 1][rx];
		int fx = map.getInt("focusX");
		int fy = map.getInt("focusY");
		state.focus = state.matrix[gridWidth - fy - 1][fx];

		// Restore the states of all the cells which are in use.
		for (int y = 0; y < gridHeight; ++y) {
			for (int x = 0; x < gridWidth; ++x) {
				if (x >= state.matrix.length || y >= state.matrix[x].length)
					return false;
				String key = "cell " + y + "," + x;
				Bundle cmap = map.getBundle(key);
				if (cmap == null)
					return false;
				state.matrix[gridWidth - x - 1][y].restoreState(cmap);
				state.matrix[gridWidth - x - 1][y].rotateImmediate(90);
			}
		}

		return true;
	}

	// ******************************************************************** //
	// Utilities.
	// ******************************************************************** //

	/**
	 * Return the given value plus one, but wrapped within the given range.
	 * 
	 * @param v
	 *            Value to increment.
	 * @param min
	 *            Minimum allowed value, inclusive.
	 * @param max
	 *            Maximum allowed value, not inclusive.
	 * @return The incremented value, wrapped around to stay in range.
	 */
	private static final int incr(int v, int min, int max) {
		return v < max - 1 ? ++v : min;
	}

	/**
	 * Return the given value minus one, but wrapped within the given range.
	 * 
	 * @param v
	 *            Value to decrement.
	 * @param min
	 *            Minimum allowed value, inclusive.
	 * @param max
	 *            Maximum allowed value, not inclusive.
	 * @return The decremented value, wrapped around to stay in range.
	 */
	private static final int decr(int v, int min, int max) {
		return v > min ? --v : max - 1;
	}

	// ******************************************************************** //
	// Private Classes.
	// ******************************************************************** //

	/**
	 * A game state. Saves a state of the game board, either to save where we
	 * are, or to record an initial or solved position.
	 */
	private class GameState {
		// Create a game state based on the given cell matrix.
		GameState(Cell[][] m) {
			matrix = m;
		}

		// Create a game state with a new cell matrix.
		GameState(int w, int h, Skill skill) {
			matrix = new Cell[w][h];
			for (int y = 0; y < h; ++y) {
				for (int x = 0; x < w; ++x) {
					Cell cell = new Cell(parentApp, BoardView.this, x, y);
					matrix[x][y] = cell;
				}
			}

			// Save the width and height of the playing board for this skill
			// level, and the board placement within the overall cell grid.
			int bw = screenConfig.getBoardWidth(skill, w, h);
			int bh = screenConfig.getBoardHeight(skill, w, h);
			int bsx = (w - bw) / 2;
			int bex = bsx + bw;
			int bsy = (h - bh) / 2;
			int bey = bsy + bh;

			boolean wrap = skill.wrapped;
			Cell u, d, l, r;
			for (int x = 0; x < w; x++) {
				for (int y = 0; y < h; y++) {
					matrix[x][y].reset(wrap ? Cell.Dir.NONE : Cell.Dir.FREE);

					// Re-calculate who this cell's neighbours are.
					u = d = l = r = null;
					if (wrap || y > bsy)
						u = matrix[x][decr(y, bsy, bey)];
					if (wrap || y < bey - 1)
						d = matrix[x][incr(y, bsy, bey)];
					if (wrap || x > bsx)
						l = matrix[decr(x, bsx, bex)][y];
					if (wrap || x < bex - 1)
						r = matrix[incr(x, bsx, bex)][y];
					matrix[x][y].setNeighbours(u, d, l, r);
				}
			}
		}

		Cell root = null;
		Cell focus = null;
		Cell[][] matrix = null;
	}

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	private static final String TAG = "netscramble";

	// Time in ms for a long screen or centre-button press.
	private static final int LONG_PRESS = 650;

	// Time a blip takes to cross half a cell, in ms.
	private static final long BLIPS_TIME = 300;

	// Rate at which we run moves in solve mode, in ms.
	private static final long SOLVE_STEP_TIME = 800;

	// Time taken to rotate a cell in solve mode, in ms.
	private static final long SOLVE_ROTATE_TIME = 350;

	// Random number generator for the game. We use a Mersenne Twister,
	// which is a high-quality and fast implementation of java.util.Random.
	// private static final Random rng = new MTRandom();
	private static final SecureRandom rng = new SecureRandom();

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The parent application.
	private NetScramble parentApp;

	// Screen configuration which matches the physical screen size.
	private Screen screenConfig = null;

	// Iff true, draw blips representing data moving through the network.
	private boolean drawBlips = true;

	// Width and height of the playing board, in cells. This is tailored
	// to suit the screen size and orientation. It should be invariant on
	// any given device except that it will rotate 90 degrees when the
	// screen rotates.
	private int gridWidth;
	private int gridHeight;

	// The Cell objects which make up the board. This matrix is gridWidth
	// by gridHeight, which is large enough to contain the game board at
	// any skill level.
	private Cell[][] cellMatrix;

	// "Solved" (i.e. initial, pre-scrambled) state of the board. This
	// is the canonical solution.
	private Bundle solvedState = null;

	// Width and height of the cells in the board, in pixels.
	private int cellWidth;
	private int cellHeight;

	// Horizontal and vertical padding used to center the board in the window.
	private int paddingX = 0;
	private int paddingY = 0;

	// Size of the game board, and offset of the first and last active cells.
	// These are set up to define the actual board area in use for a given
	// game. These change depending on the skill level.
	private int boardWidth;
	private int boardHeight;
	private int boardStartX;
	private int boardStartY;
	private int boardEndX;
	private int boardEndY;

	// Backing bitmap for the board, and a Canvas to draw in it.
	private Bitmap backingBitmap = null;
	private Canvas backingCanvas = null;

	// The skill level of the current game.
	private Skill gameSkill;

	// The rootCell cell of the layout; where the server is.
	private Cell rootCell;

	// The cell which currently has the focus.
	private Cell focusedCell;

	// Connected flags for each cell in the board; used in updateConnections().
	private boolean isConnected[][];

	// List of outstanding connected cells; used in updateConnections().
	private LinkedList<Cell> connectingCells;

	// Cell currently being pressed in a touch event.
	private Cell pressedCell = null;

	// Long press handling. The Handler gets notified after the long
	// press time has elapsed; longPressed is set to true when a long
	// press has been detected, so the subsequent up event can be ignored.
	private Handler longPressHandler = new Handler();
	private boolean longPressed = false;

	// The time in ms at which we last completed a data blip move cycle.
	private long blipsLastAdvance = 0;

	// Count of data blip generations.
	private int blipCount = 0;

	// Programed moves -- if this list is non-null and non-empty,
	// it contains a set of moves to be performed without user input.
	// Each move consists of a cell X and Y, and the number of degrees
	// to rotate the cell -- which must be either -180, -90, 90, or 180.
	private LinkedList<int[]> programmedMoves = null;

	// Time a which we executed the last move in the programme.
	private long lastProgMove = 0;

}
