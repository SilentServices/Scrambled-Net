
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


import java.util.HashMap;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;


/**
 * Error handling and reporting utilities.
 *
 * @author Ian Cameron Smith
 */
public class Errors
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an instance.  Since we're a singleton, this is private.
     * 
     * @param   context     The application context.
     */
    private Errors(Context context) {
        appContext = context;
    }
    
    
    // ******************************************************************** //
    // Exception Reporting.
    // ******************************************************************** //

    /**
     * Get the single instance of this class for the given Activity,
     * creating it if necessary.
     * 
     * @param   context     The Activity for which we want an error reporter.
     * @return              The single instance of this class.
     */
    public static Errors getInstance(Context context) {
        Errors instance = activityInstances.get(context);
        if (instance == null) {
            instance = new Errors(context);
            activityInstances.put(context, instance);
        }
        return instance;
    }
    
    
    // ******************************************************************** //
    // Exception Reporting.
    // ******************************************************************** //

    /**
     * Report an unexpected exception to the user by popping up a dialog
     * with some debug info.  Don't report the same exception more than twice,
     * and if we get floods of exceptions, just bomb out.
     * 
     * <p>This method may be called from any thread.  The reporting will be
     * deferred to the UI thread.
     * 
     * @param   context     The Activity for which we want an error reporter.
     * @param   e           The exception.
     */
    public static void reportException(Context context, final Exception e) {
        getInstance(context).reportException(e);
    }


    /**
     * Report an unexpected exception to the user by popping up a dialog
     * with some debug info.  Don't report the same exception more than twice,
     * and if we get floods of exceptions, just bomb out.
     * 
     * <p>This method may be called from any thread.  The reporting will be
     * deferred to the UI thread.
     * 
     * @param   e           The exception.
     */
    public void reportException(final Exception e) {
        final String exString = getErrorString(e);
        Log.e("Hermit", exString, e);

        if (appContext instanceof Activity) {
            ((Activity) appContext).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    reportActivityException(e, exString);
                }
            });
        } else {
            reportToastException(e, exString);
        }
    }


    // ******************************************************************** //
    // Dialog Notifications.
    // ******************************************************************** //

    /**
     * Report an unexpected exception to the user by popping up a dialog
     * with some debug info.  Don't report the same exception more than twice,
     * and if we get floods of exceptions, just bomb out.
     * 
     * <p>This method must be called from the UI thread.
     * 
     * @param   e           The exception.
     * @param   exString    A string describing the exception.
     */
    private void reportActivityException(Exception e, String exString) {
        // If we're already shutting down, ignore it.
        if (shuttingDown)
            return;
        
        String exTitle = "Unexpected Exception";
        
        // Bump the counter for this exception.
        int count = countError(exString);
        
        // Over 5 exceptions total, that's too many.
        if (exceptionTotal > 5) {
            exTitle = "Too Many Errors";
            exString += "\n\nToo many errors: closing down";
            shuttingDown = true;
        }

        // Now, if we've had fewer than three, or if we've had too many,
        // report it.
        if (shuttingDown || count < 3)
            showDialog(exTitle, exString);
    }
    
    
    private void showDialog(String title, String text) {
        AlertDialog.Builder builder = new AlertDialog.Builder(appContext);
        builder.setMessage(text)
                            .setCancelable(false)
                            .setTitle(title)
                            .setPositiveButton("OK", dialogListener);
        AlertDialog alert = builder.create();
        alert.show();
    }


    private DialogInterface.OnClickListener dialogListener =
                                    new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
            if (shuttingDown)
                ((Activity) appContext).finish();
        }
    };
    

    // ******************************************************************** //
    // Toast Notifications.
    // ******************************************************************** //

    /**
     * Report an unexpected exception to the user by popping up a dialog
     * with some debug info.  Don't report the same exception more than twice,
     * and if we get floods of exceptions, just bomb out.
     * 
     * <p>This method must be called from the UI thread.
     * 
     * @param   e           The exception.
     * @param   exString    A string describing the exception.
     */
    private void reportToastException(Exception e, String exString) {
        // If we're already shutting down, ignore it.
        if (shuttingDown)
            return;
        
        // Bump the counter for this exception.
        countError(exString);
        
        // Over 10 exceptions total, that's too many.  Don't report any
        // more.  (We can't actually shut down.)
        if (exceptionTotal > 10) {
            exString += "\n\nToo many errors: stopping reports";
            shuttingDown = true;
        }

        // Now report it.
        showToast(exString);
    }


    private void showToast(String text) {
        Toast toast = Toast.makeText(appContext, text, Toast.LENGTH_LONG);
        toast.show();
    }


    // ******************************************************************** //
    // Exception Utilities.
    // ******************************************************************** //

    private String getErrorString(Exception e) {
        StringBuilder text = new StringBuilder();

        text.append("Exception: ");
        text.append(e.getClass().getName());

        String msg = e.getMessage();
        if (msg != null)
            text.append(": \"" + msg + "\"");

        StackTraceElement[] trace = e.getStackTrace();
        if (trace != null && trace.length > 0) {
            StackTraceElement where = trace[0];
            String file = where.getFileName();
            int line = where.getLineNumber();
            if (file != null && line > 0)
                text.append("; " + file + " line " + line);
        }
        
        return text.toString();
    }
    
    
    private int countError(String text) {
        // Count the specific type of exception.
        Integer count = exceptionCounts.get(text);
        if (count == null)
            count = 1;
        else
            count = count + 1;
        exceptionCounts.put(text, count);
        
        // Count the total number of exceptions for this app.  Deduct one
        // exception for each 20 seconds that have passed.
        long now = System.currentTimeMillis();
        exceptionTotal -= (now - lastException) / (20 * 1000);
        lastException = now;
        if (exceptionTotal < 0)
            exceptionTotal = 0;
        ++exceptionTotal;
        
        return count;
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // The Errors instance for each context.
    private static HashMap<Context, Errors> activityInstances =
                                            new HashMap<Context, Errors>();
    
    // Counts of how often we've seen each exception type.
    private static HashMap<String, Integer> exceptionCounts =
                                            new HashMap<String, Integer>();

    // Time of the last exception, and the time-decaying count of exceptions.
    private static long lastException = 0;
    private static long exceptionTotal = 0;
    
    // True if we've had too many exceptions and need to shut down.
    private static boolean shuttingDown = false;
    
    
    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Application handle.
    private Context appContext = null;

}

