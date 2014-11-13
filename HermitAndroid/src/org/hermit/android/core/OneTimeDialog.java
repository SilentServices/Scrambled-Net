
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


import org.hermit.android.notice.InfoBox;

import android.app.Activity;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;


/**
 * A class which handles showing one-off notices.  This can be used for a
 * EULA, or for "new feature" notices which show once per app version.
 * 
 * <p>A benefit of this class is that it doesn't create the notice object
 * unless it needs to be shown; most times it doesn't.
 *
 * @author Ian Cameron Smith
 */
public class OneTimeDialog
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

    /**
     * Create a dialog for showing a notice, or other warnings / disclaimers,
     * once only.
     * 
     * <p>When your app starts, call {@link #showFirst()} to display
     * the dialog the first time your app runs.  This will actually show it
     * if it hasn't been seen for the current version of the app.
     * 
     * <p>To display the notice on demand, call {@link #show()}.
     * 
     * @param   parent       Our parent activity.
     * @param   name         Name for this notice.  This should be an internal
     *                       identifier; it will be used to name the preference
     *                       we use.
     * @param   title        Resource ID of the dialog title.
     * @param   text         Resource ID of the notice / warning text.
     * @param   close        Resource ID of the close button.
     */
    public OneTimeDialog(Activity parent, String name, int title, int text, int close) {
        parentApp = parent;
        
        // Save the notice name and contents.
        noticeName = name;
        noticeTitle = title;
        noticeText = text;
        noticeClose = close;
        
        // Create the preference name.
        prefName = PREF_PREFIX + noticeName + "Version";
        
        if (appUtils == null)
            appUtils = AppUtils.getInstance(parentApp);
        if (appPrefs == null)
            appPrefs = PreferenceManager.getDefaultSharedPreferences(parentApp);
    }


    // ******************************************************************** //
    // Notice Control.
    // ******************************************************************** //

    /**
     * Show the dialog if this is the first program run.
     */
    public void showFirst() {
        if (!isAccepted())
            show();
    }


    /**
     * Show the dialog unconditionally.
     */
    public void show() {
        if (noticeDialog == null) {
            noticeDialog = new InfoBox(parentApp, noticeClose) {
                @Override
                protected void okButtonPressed() {
                    setSeen();
                    super.okButtonPressed();
                }
            };
        }
        noticeDialog.show(noticeTitle, noticeText);
    }


    /**
     * Query whether the dialog has been shown to the user and accepted.
     * 
     * @return              True iff the user has seen the dialog and
     *                      clicked "OK".
     */
    protected boolean isAccepted() {
        AppUtils.Version version = appUtils.getAppVersion();
        
        int seen = -1;
        try {
            seen = appPrefs.getInt(prefName, seen);
        } catch (Exception e) { }
        
        // We consider the EULA accepted if the version seen by the user
        // most recently is the current version -- not an earlier or
        // later version.
        return seen == version.versionCode;
    }


    /**
     * Flag that this dialog has been seen, by setting a preference.
     */
    private void setSeen() {
        AppUtils.Version version = appUtils.getAppVersion();
        
        SharedPreferences.Editor editor = appPrefs.edit();
        editor.putInt(prefName, version.versionCode);
        editor.commit();
    }
    

    // ******************************************************************** //
    // Private Constants.
    // ******************************************************************** //

    // Prefix of the preference used to flag that the dialog has been seen
    // at a given app version.
    private static final String PREF_PREFIX = "org.hermit.android.core.";
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Application utilities instance, used to get app version.
    private static AppUtils appUtils = null;
    
    // Application's default shared preferences.
    private static SharedPreferences appPrefs = null;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //
    
    // Our parent activity.
    private final Activity parentApp;
    
    // Name for this notice.  This should be an internal identifier; it
    // will be used to name the preference we use.
    private final String noticeName;

    // Resource IDs of the dialog title, the notice / warning text,
    // and the close button label.
    private final int noticeTitle;
    private final int noticeText;
    private final int noticeClose;

    // Preference name for this notice.  This preference is used to store
    // the "accepted" flag.
    private final String prefName;

    // The EULA dialog.
    private InfoBox noticeDialog;

}

