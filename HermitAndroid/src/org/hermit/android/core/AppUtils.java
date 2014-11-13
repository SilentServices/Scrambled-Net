
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
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;


/**
 * This class provides some simple application-related utilities.
 */
public class AppUtils
{

	// ******************************************************************** //
	// Public Classes.
	// ******************************************************************** //

	/**
	 * Version info detail level.
	 */
	public enum Detail {
		/** Do not display. */
		NONE,
		/** Show basic name and version. */
		SIMPLE,
		/** Show debug-level detail. */
		DEBUG;
	}
	
	
	/**
	 * Information on an application version.
	 */
	public class Version {
		/** Application's pretty name.  null if unknown. */
		public CharSequence appName = null;
		
		/** Version code of the app.  -1 if unknown. */
		public int versionCode = -1;
		
		/** Version name of the app.  null if unknown. */
		public CharSequence versionName = null;
		
		/** Description either of the app or the version.  null if unknown. */
		public CharSequence appDesc = null;
	}
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Set up an app utils instance for the given activity.  This is private;
	 * users call getInstance() instead.
	 * 
	 * @param	parent			Activity for which we want information.
	 */
	private AppUtils(Activity parent) {
		parentApp = parent;
		resources = parent.getResources();
	}


	/**
	 * Get the app utils instance for this Activity.
	 * 
	 * @param	parent			Activity for which we want information.
	 * @return					The application utilities instance for this
	 * 							app.
	 */
	public static AppUtils getInstance(Activity parent) {
		if (utilsInstance == null)
			utilsInstance = new AppUtils(parent);
		return utilsInstance;
	}


	// ******************************************************************** //
	// Current App Info.
	// ******************************************************************** //
	   
    /**
     * Get the version info for the current app.
     * 
     * @return				App version info.  null if the info could
     * 						not be found.
     */
    public Version getAppVersion() {
    	// If we have the info, just return it.
    	if (appVersion != null)
    		return appVersion;
    	
    	// Get the package manager.
    	PackageManager pm = parentApp.getPackageManager();
    	
    	// Get our package name and use it to get our package info.  We
    	// don't need the optional info.
    	String pname = parentApp.getPackageName();
    	try {
        	appVersion = new Version();

        	PackageInfo pinfo = pm.getPackageInfo(pname, 0);
			appVersion.versionCode = pinfo.versionCode;
			appVersion.versionName = pinfo.versionName;
		 	
		 	// Get the pretty name and description of the app.
		 	ApplicationInfo ainfo = pinfo.applicationInfo;
		 	if (ainfo != null) {
		 		int alabel = ainfo.labelRes;
		 		if (alabel != 0)
		 			appVersion.appName = resources.getText(alabel);
			 	
		 		int dlabel = ainfo.descriptionRes;
		 		if (dlabel != 0)
		 			appVersion.appDesc = resources.getText(dlabel);
		 	}
		} catch (NameNotFoundException e) {
			appVersion = null;
		}
		
		return appVersion;
    }
    
    
    /**
     * Get a string containing the name and version info for the current
     * app's package, in a simple format.
     * 
     * @return				Descriptive name / version string.
     */
    public String getVersionString() {
    	return getVersionString(Detail.SIMPLE);
    }
    
   
    /**
     * Get a string containing the name and version info for the current
     * app's package.
     * 
     * @param detail		How much detail we want.
     * @return				Descriptive name / version string.
     */
    public String getVersionString(Detail detail) {
    	String pname = parentApp.getPackageName();
    	Version ver = getAppVersion();
    	if (ver == null)
    		return String.format("%s (no info)", pname);

    	CharSequence aname = ver.appName;
    	if (aname == null)
    		aname = "?";
    	int vcode = ver.versionCode;
    	CharSequence vname = ver.versionName;
    	if (vname == null)
    		vname = "?.?";

    	String res = null;
    	if (detail == Detail.DEBUG)
    		res = String.format("%s (%s) %s (%d)", aname, pname, vname, vcode);
    	else
    		res = String.format("%s %s", aname, vname);

    	return res;
    }
    

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //
    
    // The single instance of this class; null if not set up yet.
    private static AppUtils utilsInstance = null;

    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
 
	// Parent application context.
	private Activity parentApp;
	
	// App's resources.
	private Resources resources;
	
	// Version info for this application instance.  null if we don't
	// have it yet.
	private Version appVersion = null;
	
}

