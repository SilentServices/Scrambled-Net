
/**
 * org.hermit.android.notice: various notice dialogs for Android.
 * 
 * These classes are designed to help display notices of various kinds.
 *
 * <br>Copyright 2009-2010 Ian Cameron Smith
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


package org.hermit.android.notice;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;


/**
 * This class implements a popup error box (a subclass of AlertDialog)
 * which can be used to display an error message.
 */
public class ErrorDialog
	extends AlertDialog
{
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create an error dialog.
	 * 
	 * @param	parent		Parent application context.
	 * @param	button		The resource ID of the text for the OK button.
	 */
	public ErrorDialog(Context parent, int button) {
		this(parent, parent.getText(button));
	}


	/**
	 * Create an error dialog.
	 * 
	 * @param	parent		Parent application context.
	 * @param	button		The text for the OK button.
	 */
	public ErrorDialog(Context parent, CharSequence button) {
		super(parent);

		setIcon(android.R.drawable.ic_dialog_alert);
		setButton(button, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				okButtonPressed();
			}
		});
	}


    // ******************************************************************** //
    // Dialog control.
    // ******************************************************************** //

    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param	title			Title for the dialog.
     * @param	text			Text to display in the dialog.
     */
    public void show(String title, String text) {
    	setTitle(title);
        setMessage(text);
        show();
    }
    

    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param	text			Text to display in the dialog.
     */
    public void show(String text) {
    	setMessage(text);
        show();
    }
    

    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //

    /**
     * Called when the OK button is clicked.
     */
    void okButtonPressed() {
    	dismiss();
    }
    
}

