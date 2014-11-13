
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


import org.hermit.android.R;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.text.util.Linkify;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;


/**
 * This class implements a popup info box (a subclass of Dialog)
 * which can be used to display help text, about info, license info, etc.
 */
public class InfoBox
	extends Dialog
{
	
	// ******************************************************************** //
	// Public Constants.
	// ******************************************************************** //
	
	/** Select configurable button 1 -- the middle button. */
	public static final int BUTTON_1 = 1;
	
	/** Select configurable button 2 -- the middle button. */
	public static final int BUTTON_2 = 2;
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

    /**
     * Create an info box with a "close" button.
     * 
     * @param parent        Parent application context.
     */
    public InfoBox(Activity parent) {
        this(parent, R.string.button_close);
    }


	/**
	 * Create an info box.
	 * 
	 * @param parent		Parent application context.
	 * @param button		Resource ID of the text for the OK button.
	 */
	public InfoBox(Activity parent, int button) {
		super(parent);
		parentApp = parent;
		resources = parent.getResources();
		buttonLabel = button;
		buttonLinks = new int[3];
		
    	// Build the dialog body.
		View content = createDialog();
    	setContentView(content);
	}


    /**
     * Create the popup dialog UI.
     */
    private View createDialog() {
    	final int WCON = LinearLayout.LayoutParams.WRAP_CONTENT;
    	final int FPAR = LinearLayout.LayoutParams.FILL_PARENT;
    	
    	// Create the overall layout.
    	LinearLayout layout = new LinearLayout(parentApp);
    	layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(6, 6, 6, 6);
        
        // Construct the subtitle view, and add it to the layout.   Place an
        // underline bar below it.
        subtitleView = new TextView(parentApp);
        subtitleView.setTextSize(18f);
    	subtitleView.setVisibility(View.GONE);
        layout.addView(subtitleView, new LinearLayout.LayoutParams(FPAR, WCON));
        
        subtitleBar = new ImageView(parentApp);
        subtitleBar.setImageResource(android.R.drawable.divider_horizontal_dim_dark);
    	subtitleBar.setVisibility(View.GONE);
        layout.addView(subtitleBar, new LinearLayout.LayoutParams(FPAR, WCON));

        // Create a ScrollView to put the text in.  Shouldn't be necessary,
        // but...?
    	ScrollView tscroll = new ScrollView(parentApp);
    	tscroll.setVerticalScrollBarEnabled(true);
    	tscroll.setLayoutParams(new LinearLayout.LayoutParams(WCON, WCON, 1));
    	layout.addView(tscroll);

    	// Now create the text view and add it to the scroller.  Note: when
        // we use Linkify, it's necessary to call setTextColor() to
        // avoid all the text blacking out on click.
        textView = new TextView(parentApp);
    	textView.setTextSize(16);
    	textView.setTextColor(0xffffffff);
    	textView.setAutoLinkMask(Linkify.WEB_URLS);
    	textView.setLayoutParams(new LinearLayout.LayoutParams(FPAR, FPAR));
    	tscroll.addView(textView);

        // Add a layout to hold the buttons.
    	buttonHolder = new LinearLayout(parentApp);
    	buttonHolder.setBackgroundColor(0xf08080);
    	buttonHolder.setOrientation(LinearLayout.HORIZONTAL);
    	buttonHolder.setPadding(6, 3, 3, 3);
        layout.addView(buttonHolder,
				 	   new LinearLayout.LayoutParams(FPAR, WCON));
        
        // Add the OK button.
		Button but = new Button(parentApp);
		but.setText(buttonLabel);
		but.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				okButtonPressed();
			}
		});
		buttonHolder.addView(but, new LinearLayout.LayoutParams(WCON, WCON, 1));
    	
    	return layout;
    }
    
    
	// ******************************************************************** //
	// Configuration.
	// ******************************************************************** //

    /**
     * Set a link button on this dialog.  These are buttons that the
     * user can click to open a URL, e.g. the project page, license,
     * etc.
     * 
     * @param which				Which button to set: BUTTON_1 or BUTTON_2.
     * @param label				The button label as a resource ID.
     * @param link				Resource ID of the URL for the button.
     */
    public void setLinkButton(int which, int label, int link) {
    	final int WCON = LinearLayout.LayoutParams.WRAP_CONTENT;
    	
    	// Add the requested button, if not there yet.
    	int count = buttonHolder.getChildCount();
    	for (int i = count; i <= which; ++i) {
    		Button but = new Button(parentApp);
        	but.setId(i);
        	but.setOnClickListener(new View.OnClickListener() {
        		@Override
				public void onClick(View b) {
        			linkButtonPressed(((Button) b).getId());
        		}
        	});

        	LinearLayout.LayoutParams lp;
        	lp = new LinearLayout.LayoutParams(WCON, WCON);
        	lp.gravity = Gravity.RIGHT;
        	buttonHolder.addView(but, lp);
    	}
    	
    	// Set up the button as desired.
    	Button but = (Button) buttonHolder.getChildAt(which);
    	but.setText(label);
    	buttonLinks[which] = link;
    }
     
    
    /**
     * Set the subtitle for the about box.
     * 
     * @param textId		ID of the subtitle to display; if 0, don't show one.
     */
    public void setSubtitle(int textId) {
    	setSubtitle(textId == 0 ? null : resources.getString(textId));
    }
    
    
    /**
     * Set the subtitle for the about box.
     * 
     * @param text			Subtitle to display; if null, don't show one.
     */
    public void setSubtitle(String text) {
    	if (text == null) {
        	subtitleView.setVisibility(View.GONE);
        	subtitleBar.setVisibility(View.GONE);
    	} else {
    		subtitleView.setText(text);
    		subtitleView.setVisibility(View.VISIBLE);
    		subtitleBar.setVisibility(View.VISIBLE);
    	}
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
    public void show(int title, int text) {
    	setTitle(title);
        textView.setText(text);
        show();
    }
    

    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param	text			Text to display in the dialog.
     */
    public void show(int text) {
        textView.setText(text);
        show();
    }
    

    /**
     * Start the dialog and display it on screen.  The window is placed in
     * the application layer and opaque.
     * 
     * @param   text            Text to display in the dialog.
     */
    public void show(String text) {
        textView.setText(text);
        show();
    }
    

    // ******************************************************************** //
    // Input Handling.
    // ******************************************************************** //

    /**
     * Called when the OK button is clicked.
     */
    protected void okButtonPressed() {
    	dismiss();
    }
    

    /**
     * Called when a link button is clicked.
     * 
     * @param   which       The ID of the link button which has been
     *                      clicked, as passed to
     *                      {@link #setLinkButton(int, int, int)}.
     */
    protected void linkButtonPressed(int which) {
    	if (which < 1 || which >= buttonLinks.length)
    		return;
    	
    	int URLId = buttonLinks[which];
    	String URLText = resources.getString(URLId);
    	Intent myIntent = new Intent(android.content.Intent.ACTION_VIEW, 
                				     Uri.parse(URLText));
        parentApp.startActivity(myIntent); 
    }
    
   
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
 
	// Parent application context.
	private Activity parentApp;
	
	// App's resources.
	private Resources resources;

	// Text view we use to show the subtitle, and divider bar.
	private TextView subtitleView;
	private ImageView subtitleBar;
	
	// Text view we use to show the text.
	private TextView textView;
	
	// Layout to hold the link buttons.
	private LinearLayout buttonHolder;

	// OK button label.
	private int buttonLabel;

	// The URLs associated with the user-specified link buttons.
	private int[] buttonLinks;
	
}

