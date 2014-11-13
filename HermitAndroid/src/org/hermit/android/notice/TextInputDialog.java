
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
import android.graphics.Color;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;


/**
 * This class implements a popup input box (a subclass of AlertDialog)
 * which can be used to display a prompt and read a text string from
 * the user.
 */
public class TextInputDialog
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
		 * 
		 * @param	input			The input text.
		 */
		public void onOk(CharSequence input);
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
	public TextInputDialog(Context parent, int okBut, int cancelBut) {
		this(parent, parent.getText(okBut), parent.getText(cancelBut));
	}


	/**
	 * Create an error dialog.
	 * 
	 * @param	parent		Parent application context.
	 * @param	okBut		The text for the OK button.
	 * @param	cancelBut	The text for the cancel button.
	 */
	public TextInputDialog(Context parent, CharSequence okBut, CharSequence cancelBut) {
		super(parent);

		// Set an appropriate icon.
		setIcon(android.R.drawable.ic_dialog_info);
		
		// Set up a custom view for getting the text.
		setView(createInputView(parent));
		
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

	
	/**
	 * Create the input view for this dialog.  This is basically just a prompt
	 * and input field, both of which can be initialized when the dialog
	 * is shown.
	 * 
	 * @param	parent			Application context.
	 * @return					The input view.
	 */
	private View createInputView(Context parent) {
		LinearLayout view = new LinearLayout(parent);
		view.setOrientation(LinearLayout.VERTICAL);
		LayoutParams lp;
		
		inputPrompt = new TextView(parent);
		inputPrompt.setGravity(Gravity.LEFT);
		inputPrompt.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
		inputPrompt.setTextColor(Color.WHITE);
		inputPrompt.setText("Enter text:");
	
		lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		lp.setMargins(20, 20, 20, 10);
		view.addView(inputPrompt, lp);
		
		inputField = new EditText(parent);
		inputField.setHorizontallyScrolling(true);
		inputField.setGravity(Gravity.FILL_HORIZONTAL);
		inputField.setTextAppearance(parent, android.R.attr.textAppearanceMedium);
	
		lp = new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT);
		lp.setMargins(20, 10, 20, 20);
		view.addView(inputField, lp);
		
		return view;
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
    	show(title, text, "");
    }
    
	
    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param	title			Title for the dialog.
     * @param	text			Input prompt to display in the dialog.
     * @param	dflt			Default text to display in the input field.
     */
    public void show(int title, int text, String dflt) {
    	setTitle(title);
		inputPrompt.setText(text);
		inputField.setText(dflt);
        show();
    }
    
	
    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param	title			Title for the dialog.
     * @param	text			Input prompt to display in the dialog.
     */
    public void show(String title, String text) {
    	show(title, text, "");
    }
    
	
    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param	title			Title for the dialog.
     * @param	text			Input prompt to display in the dialog.
     * @param	dflt			Default text to display in the input field.
     */
    public void show(String title, String text, String dflt) {
    	setTitle(title);
		inputPrompt.setText(text);
		inputField.setText(dflt);
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
    		listener.onOk(inputField.getText());
    }
    

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // Input prompt label.
    private TextView inputPrompt;
    
    // Input field.
    private EditText inputField;
  
    // User's OK listener.  null if not set.
    private OnOkListener listener = null;
    
}

