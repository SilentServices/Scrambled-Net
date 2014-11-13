
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
 * This class implements a popup dialog box (a subclass of AlertDialog)
 * which can be used to display a yes / no question.
 */
public class YesNoDialog
	extends AlertDialog
{
	
	// ******************************************************************** //
	// Public Classes.
	// ******************************************************************** //
	
	/**
	 * Listener invoked when the user clicks the OK button.
	 */
	public interface OnOkListener {
		/**
		 * The OK button has been clicked.
		 */
		public void onOk();
	}
	

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create an error dialog.
	 * 
	 * @param	parent		Parent application context.
	 * @param	okBut		The resource ID of the text for the OK button.
	 * @param	cancelBut	The resource ID of the text for the cancel button.
	 */
	public YesNoDialog(Context parent, int okBut, int cancelBut) {
		this(parent, parent.getText(okBut), parent.getText(cancelBut));
	}


	/**
	 * Create an error dialog.
	 * 
	 * @param	parent		Parent application context.
	 * @param	okBut		The text for the OK button.
	 * @param	cancelBut	The text for the cancel button.
	 */
	public YesNoDialog(Context parent, CharSequence okBut, CharSequence cancelBut) {
		super(parent);
		appContext = parent;

		// Set an appropriate icon.
		setIcon(android.R.drawable.ic_dialog_info);
		
		// Set up an OK button and a cancel button.
		setButton(BUTTON_POSITIVE, okBut, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				okButtonPressed();
			}
		});
		setButton(BUTTON_NEGATIVE, cancelBut, new OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dismiss();
			}
		});
	}


    // ******************************************************************** //
    // Dialog control.
    // ******************************************************************** //

	/**
	 * Set a listener for the dialog.
	 * 
	 * @param	listener		The listener to set.
	 */
	public void setOnOkListener(OnOkListener listener) {
		this.listener = listener;
	}
	
	
    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param	title			Title for the dialog.
     * @param	text			Input prompt to display in the dialog.
     */
    public void show(int title, int text) {
    	show(appContext.getText(title), appContext.getText(text));
    }
    
	
    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param	title			Title for the dialog.
     * @param	text			Input prompt to display in the dialog.
     */
    public void show(CharSequence title, CharSequence text) {
    	setTitle(title);
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
    	if (listener != null)
    		listener.onOk();
    }
    

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // App context.
    private final Context appContext;
  
    // User's OK listener.  null if not set.
    private OnOkListener listener = null;
    
}

