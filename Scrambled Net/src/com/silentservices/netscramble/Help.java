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

import org.hermit.android.core.HelpActivity;

import android.os.Bundle;

/**
 * Simple help activity for NetScramble.
 */
public class Help extends HelpActivity {

	/**
	 * Called when the activity is starting. This is where most initialization
	 * should go: calling setContentView(int) to inflate the activity's UI, etc.
	 */
	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		// Load the preferences from an XML resource
		addHelpFromArrays(R.array.help_titles, R.array.help_texts);
	}

}
