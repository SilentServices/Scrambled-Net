
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
import android.util.AttributeSet;
import android.view.View;
import android.widget.ImageButton;


/**
 * This class displays an image button which toggles or cycles through
 * multiple states when clicked.
 */
public class MultistateImageButton
	extends ImageButton
{

	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //
	
	/**
	 * Create a multistate image button with a specified set of image
	 * resource IDs.
	 * 
	 * @param	context			Parent application.
	 * @param	images			Resource IDs of the images to use for each
	 * 							state.
	 */
	public MultistateImageButton(Context context, int[] images) {
		super(context);
		init(images);
	}

	
	/**
	 * Create a multistate image button with a specified set of image
	 * resource IDs.
	 * 
	 * @param	context			Parent application.
	 * @param	attrs			Layout attributes.
	 * @param	images			Resource IDs of the images to use for each
	 * 							state.
	 */
	public MultistateImageButton(Context context, AttributeSet attrs, int[] images) {
		super(context, attrs);
		init(images);
	}

	
	private void init(int[] images) {
		imageIds = images;
		
		setClickable(true);
		setState(0);
		
		// Register ourselves as the listener for clicks.
		super.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View arg0) {
				setState((currentState + 1) % imageIds.length);
				if (clientListener != null)
					clientListener.onClick(arg0);
			}
		});
	}

	
	// ******************************************************************** //
	// Listener.
	// ******************************************************************** //
	
	/**
	 * Register a callback to be invoked when this view is clicked.
	 * If this view is not clickable, it becomes clickable.
	 * 
	 * We override this here because we are using the parent class's
	 * listener slot for our own purposes.
	 * 
	 * @param	l				The callback that will run.
	 */
	@Override
	public void setOnClickListener(OnClickListener l) {
		clientListener = l;
		setClickable(true);
	}

	  
	// ******************************************************************** //
	// Control.
	// ******************************************************************** //
	
	/**
	 * Get the current state of this button.
	 * 
	 * @return				The current state, as an index into the list
	 * 						of images.
	 */
	public int getState() {
		return currentState;
	}
	

	/**
	 * Set the current state of this button.
	 * 
	 * @param	s			State to set, as an index into the list of images.
	 */
	public void setState(int s) {
		currentState = s;
		
		setImageResource(imageIds[currentState]);
	}
	

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "onwatch";
    

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The resource IDs of the images for each state.
	private int[] imageIds;
	
	// Listener which the client has registered for click events.  Null
	// if not set.
	private OnClickListener clientListener = null;

	// The current state, as an index into imageIDs.
	private int currentState;

}

