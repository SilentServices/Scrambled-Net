/**
 * org.hermit.android.core: useful Android foundation classes.
 * 
 * These classes are designed to help build various types of application.
 *
 * <br>Copyright 2009-2010 Ian Cameron Smith
 *
 * * © 2014 Michael Mueller <michael.mueller@silentservices.de>
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

import org.hermit.android.R;
import org.hermit.android.notice.InfoBox;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * An enhanced Activity class, for use as the main activity of an application.
 * The main thing this class provides is a nice callback-based mechanism for
 * starting sub-activities. This makes it easier for different parts of an app
 * to kick off sub-activities and get the results.
 * 
 * <p>
 * Note: it is best that sub-classes do not implement onActivityResult(int, int,
 * Intent). If they do, then for safety use small request codes, and call
 * super.onActivityResult(int, int, Intent) when you get an unknown code.
 * 
 * @author Ian Cameron Smith
 */
public class MainActivity extends Activity {

	// ******************************************************************** //
	// Public Classes.
	// ******************************************************************** //

	/**
	 * This interface defines a listener for sub-activity results.
	 */
	public static abstract class ActivityListener {

		/**
		 * Called when an activity you launched exits.
		 * 
		 * <p>
		 * Applications can override this to be informed when an activity
		 * finishes, either by an error, the user pressing "back", or normally,
		 * or whatever. The default implementation calls either
		 * onActivityCanceled(), if resultCode == RESULT_CANCELED, or else
		 * onActivityResult().
		 * 
		 * @param resultCode
		 *            The integer result code returned by the child activity
		 *            through its setResult().
		 * @param data
		 *            Additional data returned by the activity.
		 */
		public void onActivityFinished(int resultCode, Intent data) {
			if (resultCode == RESULT_CANCELED)
				onActivityCanceled(data);
			else
				onActivityResult(resultCode, data);
		}

		/**
		 * Called when an activity you launched exits with a result code of
		 * RESULT_CANCELED. This will happen if the user presses "back", or if
		 * the activity returned that code explicitly, didn't return any result,
		 * or crashed during its operation.
		 * 
		 * <p>
		 * Applications can override this if they want to be separately notified
		 * of a RESULT_CANCELED. It doesn't make sense to override both
		 * onActivityFinished() and this method.
		 * 
		 * @param data
		 *            Additional data returned by the activity.
		 */
		public void onActivityCanceled(Intent data) {
		}

		/**
		 * Called when an activity you launched exits with a result code other
		 * than RESULT_CANCELED, giving you the resultCode it returned, and any
		 * additional data from it.
		 * 
		 * <p>
		 * Applications can override this if they want to be separately notified
		 * of a normal exit. It doesn't make sense to override both
		 * onActivityFinished() and this method.
		 * 
		 * @param resultCode
		 *            The integer result code returned by the child activity
		 *            through its setResult().
		 * @param data
		 *            Additional data returned by the activity.
		 */
		public void onActivityResult(int resultCode, Intent data) {
		}

		// This listener's request code. This code is auto-assigned
		// the first time the listener is used, and is used to find it
		// from the response.
		private int requestCode = 0;

	}

	// ******************************************************************** //
	// Activity Lifecycle.
	// ******************************************************************** //

	/**
	 * Called when the activity is starting. This is where most initialisation
	 * should go: calling setContentView(int) to inflate the activity's UI, etc.
	 * 
	 * You can call finish() from within this function, in which case
	 * onDestroy() will be immediately called without any of the rest of the
	 * activity lifecycle executing.
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
	 * 
	 * @param icicle
	 *            If the activity is being re-initialised after previously being
	 *            shut down then this Bundle contains the data it most recently
	 *            supplied in onSaveInstanceState(Bundle). Note: Otherwise it is
	 *            null.
	 */
	@Override
	protected void onCreate(Bundle icicle) {
		super.onCreate(icicle);

		appUtils = AppUtils.getInstance(this);
		errorReporter = Errors.getInstance(this);
	}

	// ******************************************************************** //
	// EULA Dialog.
	// ******************************************************************** //

	/**
	 * Create a dialog for showing the EULA, or other warnings / disclaimers.
	 * When your app starts, call {@link #showFirstEula()} to display the dialog
	 * the first time your app runs. To display it on demand, call
	 * {@link #showEula()}.
	 * 
	 * @param title
	 *            Resource ID of the dialog title.
	 * @param text
	 *            Resource ID of the EULA / warning text.
	 * @param close
	 *            Resource ID of the close button.
	 */
	public void createEulaBox(int title, int text, int close) {
		eulaDialog = new OneTimeDialog(this, "eula", title, text, close);
	}

	/**
	 * Show the EULA dialog if this is the first program run. You need to have
	 * created the dialog by calling {@link #createEulaBox(int, int, int)}.
	 */
	public void showFirstEula() {
		if (eulaDialog != null)
			eulaDialog.showFirst();
	}

	/**
	 * Show the EULA dialog unconditionally. You need to have created the dialog
	 * by calling {@link #createEulaBox(int, int, int)}.
	 */
	public void showEula() {
		if (eulaDialog != null)
			eulaDialog.show();
	}

	// ******************************************************************** //
	// Help and About Boxes.
	// ******************************************************************** //

	/**
	 * Create a dialog for help / about boxes etc. If you want to display one of
	 * those, set up the info in it by calling {@link #setHomeInfo(int, int)},
	 * {@link #setAboutInfo(int)} and {@link #setLicenseInfo(int, int)}; then
	 * pop up a dialog by calling {@link #showAbout()}.
	 * 
	 * @param close
	 *            Resource ID of the close button.
	 * @deprecated The message box is now created automatically.
	 */
	@Deprecated
	public void createMessageBox(int close) {
		messageDialog = new InfoBox(this, close);
		String version = appUtils.getVersionString();
		messageDialog.setTitle(version);
	}

	/**
	 * Set up the about info for dialogs. See {@link #showAbout()}.
	 * 
	 * @param about
	 *            Resource ID of the about text.
	 */
	public void setAboutInfo(int about) {
		aboutText = about;
	}

	/**
	 * Set up the homepage info for dialogs. See {@link #showAbout()}.
	 * 
	 * @param link
	 *            Resource ID of the URL the button links to.
	 */
	public void setHomeInfo(int link) {
		homeButton = R.string.button_homepage;
		homeLink = link;
	}

	/**
	 * Set up the homepage info for dialogs. See {@link #showAbout()}.
	 * 
	 * @param button
	 *            Resource ID of the button text.
	 * @param link
	 *            Resource ID of the URL the button links to.
	 */
	@Deprecated
	public void setHomeInfo(int button, int link) {
		homeButton = button;
		homeLink = link;
	}

	/**
	 * Set up the license info for dialogs. See {@link #showAbout()}.
	 * 
	 * @param link
	 *            Resource ID of the URL the button links to.
	 */
	public void setLicenseInfo(int link) {
		licButton = R.string.button_license;
		licLink = link;
	}

	/**
	 * Set up the license info for dialogs. See {@link #showAbout()}.
	 * 
	 * @param button
	 *            Resource ID of the button text.
	 * @param link
	 *            Resource ID of the URL the button links to.
	 */
	@Deprecated
	public void setLicenseInfo(int button, int link) {
		licButton = button;
		licLink = link;
	}

	/**
	 * Show an about dialog. You need to have configured it by calling
	 * {@link #setAboutInfo(int)}, {@link #setHomeInfo(int, int)} and
	 * {@link #setLicenseInfo(int, int)}.
	 */
	public void showAbout() {
		// Create the dialog the first time.
		if (messageDialog == null)
			createMessageBox();

		messageDialog.setLinkButton(1, homeButton, homeLink);
		if (licButton != 0 && licLink != 0)
			messageDialog.setLinkButton(2, licButton, licLink);
		messageDialog.show(aboutText);
	}

	/**
	 * Create a dialog for help / about boxes etc. If you want to display one of
	 * those, set up the info in it by calling {@link #setHomeInfo(int, int)},
	 * {@link #setAboutInfo(int)} and {@link #setLicenseInfo(int, int)}; then
	 * pop up a dialog by calling {@link #showAbout()}.
	 */
	private void createMessageBox() {
		messageDialog = new InfoBox(this);
		String version = appUtils.getVersionString();
		messageDialog.setTitle(version);
	}

	// ******************************************************************** //
	// Exception Reporting.
	// ******************************************************************** //

	/**
	 * Report an unexpected exception to the user by popping up a dialog with
	 * some debug info. Don't report the same exception more than twice, and if
	 * we get floods of exceptions, just bomb out.
	 * 
	 * <p>
	 * This method may be called from any thread. The reporting will be deferred
	 * to the UI thread.
	 * 
	 * @param e
	 *            The exception.
	 */
	public void reportException(final Exception e) {
		errorReporter.reportException(e);
	}

	// ******************************************************************** //
	// Sub-Activities.
	// ******************************************************************** //

	/**
	 * Launch an activity for which you would like a result when it finished.
	 * When this activity exits, the given ActivityListener will be invoked.
	 * 
	 * <p>
	 * Note that this method should only be used with Intent protocols that are
	 * defined to return a result. In other protocols (such as ACTION_MAIN or
	 * ACTION_VIEW), you may not get the result when you expect.
	 * 
	 * As a special case, if you call startActivityForResult() during the
	 * initial onCreate() / onResume() of your activity, then your window will
	 * not be displayed until a result is returned back from the started
	 * activity.
	 * 
	 * This method throws ActivityNotFoundException if there was no Activity
	 * found to run the given Intent.
	 * 
	 * @param intent
	 *            The intent to start.
	 * @param listener
	 *            Listener to invoke when the activity returns.
	 */
	public void startActivityForResult(Intent intent, ActivityListener listener) {
		// If this listener doesn't yet have a request code, give it one,
		// and add it to the map so we can find it again. On subsequent calls
		// we re-use the same code.
		if (listener.requestCode == 0) {
			listener.requestCode = nextRequest++;
			codeMap.put(listener.requestCode, listener);
		}

		// Start the sub-activity.
		startActivityForResult(intent, listener.requestCode);
	}

	// ******************************************************************** //
	// Activity Management.
	// ******************************************************************** //

	/**
	 * Called when an activity you launched exits, giving you the requestCode
	 * you started it with, the resultCode it returned, and any additional data
	 * from it. The resultCode will be RESULT_CANCELED if the activity
	 * explicitly returned that, didn't return any result, or crashed during its
	 * operation.
	 * 
	 * @param requestCode
	 *            The integer request code originally supplied to
	 *            startActivityForResult(), allowing you to identify who this
	 *            result came from.
	 * @param resultCode
	 *            The integer result code returned by the child activity through
	 *            its setResult().
	 * @param data
	 *            Additional data to return to the caller.
	 */
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		ActivityListener listener = codeMap.get(requestCode);
		if (listener == null)
			Log.e("MainActivity", "Unknown request code: " + requestCode);
		else
			listener.onActivityFinished(resultCode, data);
	}

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Application utilities instance, used to get app version.
	private AppUtils appUtils = null;

	// Exception reporter.
	private Errors errorReporter;

	// The next request code available to be used. Our request codes
	// start at a large number, for no special reason.
	private int nextRequest = 0x60000000;

	// This map translates request codes to the listeners registered for
	// those requests. It is used when a response is received to activate
	// the correct listener.
	private HashMap<Integer, ActivityListener> codeMap = new HashMap<Integer, ActivityListener>();

	// The EULA dialog. Null if the user hasn't set one up.
	private OneTimeDialog eulaDialog = null;

	// Dialog used to display about etc.
	private InfoBox messageDialog = null;

	// IDs of the button strings and URLs for "Home" and "License".
	private int homeButton = 0;
	private int homeLink = 0;
	private int licButton = 0;
	private int licLink = 0;

	// ID of the about text.
	private int aboutText = 0;

}
