
/**
 * org.hermit.android.core: useful Android foundation classes.
 * 
 * These classes are designed to help build various types of application.
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


package org.hermit.android.core;


import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;


/**
 * An activity which displays a splash screen and then returns to the
 * calling activity.
 */
public class SplashActivity
	extends Activity
{

	// ******************************************************************** //
    // Public Constants.
    // ******************************************************************** //

    /**
     * Extras key for the image resource ID.  The extras data named
     * by this key is an int which specifies the image to display.
     */
    public static final String EXTRAS_IMAGE_ID = "image_id";

    /**
     * Extras key for the splash screen display time.  The extras data named
     * by this key is the time in ms for which the splash screen should be
     * displayed.
     */
    public static final String EXTRAS_TIME_ID = "splash_time";
    
    
    // ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

	/**
	 * Called when the activity is starting.  This is where most
	 * initialization should go: calling setContentView(int) to inflate
	 * the activity's UI, etc.
	 * 
	 * @param	icicle			Saved application state, if any.
	 */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        // We don't want a title bar or status bar.
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        
        // Get our parameters from the intent.
        Intent intent = getIntent();
        int imageId = intent.getIntExtra(EXTRAS_IMAGE_ID, 0);
        long time = intent.getLongExtra(EXTRAS_TIME_ID, 3000);
        
        // Create the splash view.
        ImageView splashView = new ImageView(this);
        splashView.setScaleType(ScaleType.CENTER_CROP);
        splashView.setImageResource(imageId);
        setContentView(splashView);
        
        // On click, finish.
        splashView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        // Set up a handler that closes the splash screen.
        Handler splashHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                finish();
            }
        };
        splashHandler.sendEmptyMessageDelayed(1, time);
    }

    
    // ******************************************************************** //
    // Convenience Methods.
    // ******************************************************************** //
    
    /**
     * Launch a splash screen displaying the given drawable.
     * 
     * @param   context         Application context we're running in.
     * @param   image           Resource ID of the image to display.
     * @param   time            Time in ms for which the splash screen should
     *                          be visible.
     */
    public static void launch(Context context, int image, long time) {
        Intent intent = new Intent(context, SplashActivity.class);
        intent.putExtra(SplashActivity.EXTRAS_IMAGE_ID, image);
        intent.putExtra(SplashActivity.EXTRAS_TIME_ID, time);
        context.startActivity(intent);
    }

}

