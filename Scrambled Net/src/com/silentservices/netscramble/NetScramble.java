/**
 * NetScramble: unscramble a network and connect all the terminals.
 * The player is given a network diagram with the parts of the network
 * randomly rotated; he/she must rotate them to connect all the terminals
 * to the server.
 * 
 * This is an Android implementation of the KDE game "knetwalk" by
 * Andi Peredri, Thomas Nagy, and Reinhold Kainhofer.
 *
 * © 2007-2010 Ian Cameron Smith <johantheghost@yahoo.com>
 *
 * © 2014 Michael Mueller <michael.mueller@silentservices.de>
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

package com.silentservices.netscramble;

import org.hermit.android.core.AppUtils;
import org.hermit.android.core.MainActivity;
import org.hermit.android.core.OneTimeDialog;
import org.hermit.android.notice.InfoBox;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.silentservices.netscramble.BoardView.Skill;

/**
 * Main NetScramble activity.
 */
public class NetScramble extends ActionBarActivity {

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Application utilities instance, used to get app version.
	private AppUtils appUtils = null;
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

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * Current state of the game.
	 */
	static enum State {
		NEW, RESTORED, INIT, PAUSED, RUNNING, SOLVED, ABORTED;

		static State getValue(int ordinal) {
			return states[ordinal];
		}

		private static State[] states = values();
	}

	/**
	 * The sounds that we make.
	 */
	static enum Sound {
		START(R.raw.start), CLICK(R.raw.click), TURN(R.raw.turn), CONNECT(
				R.raw.connect), POP(R.raw.pop), WIN(R.raw.win);

		private Sound(int res) {
			soundRes = res;
		}

		private final int soundRes; // Resource ID for the sound file.
		private int soundId = 0; // Sound ID for playing.
	}

	/**
	 * Sound play mode.
	 */
	static enum SoundMode {
		NONE(R.id.sounds_off), QUIET(R.id.sounds_qt), FULL(R.id.sounds_on);

		private SoundMode(int res) {
			menuId = res;
		}

		private int menuId; // ID of the corresponding menu item.
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
	// Activity Setup.
	// ******************************************************************** //

	/**
	 * Called when the activity is starting. This is where most initialization
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
	 *            If the activity is being re-initialized after previously being
	 *            shut down then this Bundle contains the data it most recently
	 *            supplied in onSaveInstanceState(Bundle). Note: Otherwise it is
	 *            null.
	 */
	@Override
	public void onCreate(Bundle icicle) {
		Log.i(TAG, "onCreate(): "
				+ (icicle == null ? "clean start" : "restart"));

		super.onCreate(icicle);

		appUtils = AppUtils.getInstance(this);

		// Set up the standard dialogs.
		setAboutInfo(R.string.about_text);
		setHomeInfo(R.string.url_homepage);
		setLicenseInfo(R.string.url_license);

		// Create our EULA box.
		createEulaBox(R.string.eula_title, R.string.eula_text,
				R.string.button_close);
		appResources = getResources();

		gameTimer = new GameTimer();

		// We don't want a title bar.
		// getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
		// WindowManager.LayoutParams.FLAG_FULLSCREEN);
		// requestWindowFeature(Window.FEATURE_NO_TITLE);

		// We want the audio controls to control our sound volume.
		this.setVolumeControlStream(AudioManager.STREAM_MUSIC);

		// Create string formatting buffers.
		clicksText = new StringBuilder(10);
		timeText = new StringBuilder(10);

		// Create the GUI for the game.
		setContentView(R.layout.main);
		setupGui();

		// Restore our preferences.
		SharedPreferences prefs = getPreferences(0);

		// See if sounds are enabled and how. The old way was boolean
		// soundMode, so look for that as a fallback.
		soundMode = SoundMode.FULL;
		{
			String smode = prefs.getString("soundMode", null);
			if (smode != null)
				soundMode = SoundMode.valueOf(smode);
			else {
				String son = prefs.getString("soundEnable", null);
				if (son != null)
					soundMode = Boolean.valueOf(son) ? SoundMode.FULL
							: SoundMode.NONE;
			}
		}

		// See if animations are enabled.
		animEnable = prefs.getBoolean("animEnable", true);
		boardView.setAnimEnable(animEnable);

		// Load the sounds.
		soundPool = createSoundPool();

		// If we have a previous state to restore, try to do so.
		boolean restored = false;
		if (icicle != null)
			restored = restoreState(icicle);

		// Get the current game skill level from the preferences, if we didn't
		// get a saved game. Default to NOVICE if it's not there.
		if (!restored) {
			gameSkill = null;
			String skill = prefs.getString("skillLevel", null);
			if (skill != null)
				gameSkill = Skill.valueOf(skill);
			if (gameSkill == null)
				gameSkill = Skill.NOVICE;
			gameState = State.NEW;
		} else {
			// Save our restored game state.
			restoredGameState = gameState;
			gameState = State.RESTORED;
		}
	}

	/**
	 * Called when the current activity is being re-displayed to the user (the
	 * user has navigated back to it). It will be followed by onStart().
	 * 
	 * For activities that are using raw Cursor objects (instead of creating
	 * them through managedQuery(android.net.Uri, String[], String, String[],
	 * String), this is usually the place where the cursor should be requeried
	 * (because you had deactivated it in onStop().
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
	 */
	@Override
	protected void onRestart() {
		Log.i(TAG, "onRestart()");

		super.onRestart();
	}

	/**
	 * Called after onCreate(Bundle) or onStop() when the current activity is
	 * now being displayed to the user. It will be followed by onResume() if the
	 * activity comes to the foreground, or onStop() if it becomes hidden.
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
	 */
	@Override
	protected void onStart() {
		Log.i(TAG, "onStart()");
		super.onStart();

		boardView.onStart();
	}

	/**
	 * This method is called after onStart() when the activity is being
	 * re-initialized from a previously saved state, given here in state. Most
	 * implementations will simply use onCreate(Bundle) to restore their state,
	 * but it is sometimes convenient to do it here after all of the
	 * initialization has been done or to allow subclasses to decide whether to
	 * use your default implementation. The default implementation of this
	 * method performs a restore of any view state that had previously been
	 * frozen by onSaveInstanceState(Bundle).
	 * 
	 * This method is called between onStart() and onPostCreate(Bundle).
	 * 
	 * @param inState
	 *            The data most recently supplied in
	 *            onSaveInstanceState(Bundle).
	 */
	@Override
	protected void onRestoreInstanceState(Bundle inState) {
		Log.i(TAG, "onRestoreInstanceState()");

		super.onRestoreInstanceState(inState);

		// Save the state.
		// saveState(outState);
	}

	/**
	 * Called after onRestoreInstanceState(Bundle), onRestart(), or onPause(),
	 * for your activity to start interacting with the user. This is a good
	 * place to begin animations, open exclusive-access devices (such as the
	 * camera), etc.
	 * 
	 * Keep in mind that onResume is not the best indicator that your activity
	 * is visible to the user; a system window such as the keyguard may be in
	 * front. Use onWindowFocusChanged(boolean) to know for certain that your
	 * activity is visible to the user (for example, to resume a game).
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
	 */
	@Override
	protected void onResume() {
		Log.i(TAG, "onResume()");

		super.onResume();

		// First time round, show the EULA.
		showFirstEula();
		// ActionBarActivity { //

		// Display the skill level.
		statusMode.setText(gameSkill.label);

		// If we restored a state, go to that state. Otherwise start
		// at the welcome screen.
		if (gameState == State.NEW) {
			Log.d(TAG, "onResume() NEW: init");
			setState(State.INIT, true);
		} else if (gameState == State.RESTORED) {
			Log.d(TAG, "onResume() RESTORED: set " + restoredGameState);
			setState(restoredGameState, true);

			// If we restored an aborted state, that means we were starting
			// a game. Kick it off again.
			if (restoredGameState == State.ABORTED) {
				Log.d(TAG, "onResume() RESTORED ABORTED: start");
				startGame(null);
			}
		} else if (gameState == State.PAUSED) {
			// We just paused without closing down. Resume.
			setState(State.RUNNING, true);
		} else {
			Log.e(TAG, "onResume() !!" + gameState + "!!: init");
			// setState(State.INIT); // Shouldn't get here.
		}

		boardView.onResume();
	}

	/**
	 * Called to retrieve per-instance state from an activity before being
	 * killed so that the state can be restored in onCreate(Bundle) or
	 * onRestoreInstanceState(Bundle) (the Bundle populated by this method will
	 * be passed to both).
	 * 
	 * This method is called before an activity may be killed so that when it
	 * comes back some time in the future it can restore its state.
	 * 
	 * Do not confuse this method with activity lifecycle callbacks such as
	 * onPause(), which is always called when an activity is being placed in the
	 * background or on its way to destruction, or onStop() which is called
	 * before destruction.
	 * 
	 * The default implementation takes care of most of the UI per-instance
	 * state for you by calling onSaveInstanceState() on each view in the
	 * hierarchy that has an id, and by saving the id of the currently focused
	 * view (all of which is restored by the default implementation of
	 * onRestoreInstanceState(Bundle)). If you override this method to save
	 * additional information not captured by each individual view, you will
	 * likely want to call through to the default implementation, otherwise be
	 * prepared to save all of the state of each view yourself.
	 * 
	 * If called, this method will occur before onStop(). There are no
	 * guarantees about whether it will occur before or after onPause().
	 * 
	 * @param outState
	 *            A Bundle in which to place any state information you wish to
	 *            save.
	 */
	@Override
	public void onSaveInstanceState(Bundle outState) {
		Log.i(TAG, "onSaveInstanceState()");

		super.onSaveInstanceState(outState);

		// Save the state.
		saveState(outState);
	}

	/**
	 * Called as part of the activity lifecycle when an activity is going into
	 * the background, but has not (yet) been killed. The counterpart to
	 * onResume().
	 * 
	 * When activity B is launched in front of activity A, this callback will be
	 * invoked on A. B will not be created until A's onPause() returns, so be
	 * sure to not do anything lengthy here.
	 * 
	 * This callback is mostly used for saving any persistent state the activity
	 * is editing, to present a "edit in place" model to the user and making
	 * sure nothing is lost if there are not enough resources to start the new
	 * activity without first killing this one. This is also a good place to do
	 * things like stop animations and other things that consume a noticeable
	 * mount of CPU in order to make the switch to the next activity as fast as
	 * possible, or to close resources that are exclusive access such as the
	 * camera.
	 * 
	 * In situations where the system needs more memory it may kill paused
	 * processes to reclaim resources. Because of this, you should be sure that
	 * all of your state is saved by the time you return from this function. In
	 * general onSaveInstanceState(Bundle) is used to save per-instance state in
	 * the activity and this method is used to store global persistent data (in
	 * content providers, files, etc.).
	 * 
	 * After receiving this call you will usually receive a following call to
	 * onStop() (after the next activity has been resumed and displayed),
	 * however in some cases there will be a direct call back to onResume()
	 * without going through the stopped state.
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
	 */
	@Override
	protected void onPause() {
		Log.i(TAG, "onPause()");
		super.onPause();

		boardView.onPause();

		// Pause the game. Don't show a splash screen because the
		// game is going away.
		if (gameState == State.RUNNING)
			setState(State.PAUSED, false);
	}

	/**
	 * Called when you are no longer visible to the user. You will next receive
	 * either onStart(), onDestroy(), or nothing, depending on later user
	 * activity.
	 * 
	 * Note that this method may never be called, in low memory situations where
	 * the system does not have enough memory to keep your activity's process
	 * running after its onPause() method is called.
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
	 */
	@Override
	protected void onStop() {
		Log.i(TAG, "onStop()");
		super.onStop();

		boardView.onStop();
	}

	/**
	 * Perform any final cleanup before an activity is destroyed. This can
	 * happen either because the activity is finishing (someone called finish()
	 * on it, or because the system is temporarily destroying this instance of
	 * the activity to save space. You can distinguish between these two
	 * scenarios with the isFinishing() method.
	 * 
	 * Note: do not count on this method being called as a place for saving
	 * data! For example, if an activity is editing data in a content provider,
	 * those edits should be committed in either onPause() or
	 * onSaveInstanceState(Bundle), not here. This method is usually implemented
	 * to free resources like threads that are associated with an activity, so
	 * that a destroyed activity does not leave such things around while the
	 * rest of its application is still running. There are situations where the
	 * system will simply kill the activity's hosting process without calling
	 * this method (or any others) in it, so it should not be used to do things
	 * that are intended to remain around after the process goes away.
	 * 
	 * Derived classes must call through to the super class's implementation of
	 * this method. If they do not, an exception will be thrown.
	 */
	@Override
	protected void onDestroy() {
		Log.i(TAG, "onDestroy()");

		super.onDestroy();
	}

	// ******************************************************************** //
	// GUI Creation.
	// ******************************************************************** //

	/**
	 * Set up the GUI for the game. Add handlers and animations where needed.
	 */
	private void setupGui() {
		viewSwitcher = (ViewAnimator) findViewById(R.id.view_switcher);

		// Make the animations for the switcher.
		animSlideInLeft = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
				1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f);
		animSlideInLeft.setDuration(ANIM_TIME);
		animSlideOutLeft = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, -1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f);
		animSlideOutLeft.setDuration(ANIM_TIME);
		animSlideInRight = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
				-1.0f, Animation.RELATIVE_TO_SELF, 0.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f);
		animSlideInRight.setDuration(ANIM_TIME);
		animSlideOutRight = new TranslateAnimation(Animation.RELATIVE_TO_SELF,
				0.0f, Animation.RELATIVE_TO_SELF, 1.0f,
				Animation.RELATIVE_TO_SELF, 0.0f, Animation.RELATIVE_TO_SELF,
				0.0f);
		animSlideOutRight.setDuration(ANIM_TIME);

		// Get handles on the important widgets.
		splashText = (TextView) findViewById(R.id.splash_text);
		boardView = (BoardView) findViewById(R.id.board_view);

		// Create the left status field.
		statusClicks = (TextView) findViewById(R.id.status_clicks);
		statusMode = (TextView) findViewById(R.id.status_mode);
		statusTime = (TextView) findViewById(R.id.status_time);

		// Set up the splash text view to call wakeUp() when
		// the user presses a key or taps the screen.
		splashText.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					wakeUp();
					return true;
				} else
					return false;
			}
		});
		splashText.setOnKeyListener(new View.OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (event.getAction() == KeyEvent.ACTION_DOWN) {
					wakeUp();
					return true;
				}
				return false;
			}
		});

		// If we have a soft menu button (which depends on the screen size),
		// then set it up.
		ImageButton menuButton = (ImageButton) findViewById(R.id.menu_button);
		if (menuButton != null) {
			menuButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					openOptionsMenu();
				}
			});
		}
	}

	// ******************************************************************** //
	// Menu Management.
	// ******************************************************************** //

	/**
	 * Initialize the contents of the game's options menu by adding items to the
	 * given menu.
	 * 
	 * This is only called once, the first time the options menu is displayed.
	 * To update the menu every time it is displayed, see
	 * onPrepareOptionsMenu(Menu).
	 * 
	 * When we add items to the menu, we can either supply a Runnable to receive
	 * notification of selection, or we can implement the Activity's
	 * onOptionsItemSelected(Menu.Item) method to handle them there.
	 * 
	 * @param menu
	 *            The options menu in which we should place our items. We can
	 *            safely hold on this (and any items created from it), making
	 *            modifications to it as desired, until the next time
	 *            onCreateOptionsMenu() is called.
	 * @return true for the menu to be displayed; false to suppress showing it.
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		mainMenu = menu;

		// We must call through to the base implementation.
		super.onCreateOptionsMenu(menu);

		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main_menu, menu);

		// GUI is created, state is restored (if any) -- now is a good time
		// to re-sync the options menus.
		selectCurrentSkill();
		selectSoundMode();
		selectAnimEnable();

		return true;
	}

	private void selectCurrentSkill() {
		// Set the selected skill menu item to the current skill.
		if (mainMenu != null) {
			MenuItem skillItem = mainMenu.findItem(gameSkill.id);
			if (skillItem != null)
				skillItem.setChecked(true);
		}
	}

	private void selectSoundMode() {
		// Set the sound enable menu item to the current state.
		if (mainMenu != null) {
			int id = soundMode.menuId;
			MenuItem soundItem = mainMenu.findItem(id);
			if (soundItem != null)
				soundItem.setChecked(true);
		}
	}

	private void selectAnimEnable() {
		// Set the animation enable menu item to the current state.
		if (mainMenu != null) {
			int id = animEnable ? R.id.anim_on : R.id.anim_off;
			MenuItem animItem = mainMenu.findItem(id);
			if (animItem != null)
				animItem.setChecked(true);
		}
	}

	void selectAutosolveMode(boolean solving) {
		// Set the autosolve menu item to the current state.
		if (mainMenu != null) {
			MenuItem solveItem = mainMenu.findItem(R.id.menu_autosolve);
			if (solveItem != null) {
				if (solving) {
					solveItem.setIcon(R.drawable.ic_menu_stop);
					solveItem.setTitle(R.string.menu_stopsolve);
				} else {
					solveItem.setIcon(R.drawable.ic_menu_solve);
					solveItem.setTitle(R.string.menu_autosolve);
				}
			}
		}
	}

	/**
	 * This hook is called whenever an item in your options menu is selected.
	 * Derived classes should call through to the base class for it to perform
	 * the default menu handling. (True?)
	 * 
	 * @param item
	 *            The menu item that was selected.
	 * @return false to have the normal processing happen.
	 */
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_new:
			startGame(null);
			break;
		case R.id.menu_pause:
			setState(State.PAUSED, true);
			break;
		case R.id.menu_scores:
			// Launch the high scores activity as a subactivity.
			setState(State.PAUSED, false);
			Intent sIntent = new Intent();
			sIntent.setClass(this, ScoreList.class);
			startActivity(sIntent);
			break;
		case R.id.menu_help:
			// Launch the help activity as a subactivity.
			setState(State.PAUSED, false);
			Intent hIntent = new Intent();
			hIntent.setClass(this, Help.class);
			startActivity(hIntent);
			break;
		case R.id.menu_about:
			showAbout();
			break;
		case R.id.skill_novice:
			startGame(Skill.NOVICE);
			break;
		case R.id.skill_normal:
			startGame(Skill.NORMAL);
			break;
		case R.id.skill_expert:
			startGame(Skill.EXPERT);
			break;
		case R.id.skill_master:
			startGame(Skill.MASTER);
			break;
		case R.id.skill_insane:
			startGame(Skill.INSANE);
			break;
		case R.id.sounds_off:
			setSoundMode(SoundMode.NONE);
			break;
		case R.id.sounds_qt:
			setSoundMode(SoundMode.QUIET);
			break;
		case R.id.sounds_on:
			setSoundMode(SoundMode.FULL);
			break;
		case R.id.anim_off:
			setAnimEnable(false);
			break;
		case R.id.anim_on:
			setAnimEnable(true);
			break;
		case R.id.menu_autosolve:
			solverUsed = true;
			boardView.autosolve();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}

		return true;
	}

	private void setSoundMode(SoundMode mode) {
		soundMode = mode;

		// Save the new setting to prefs.
		SharedPreferences prefs = getPreferences(0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("soundMode", "" + soundMode);
		editor.commit();

		selectSoundMode();
	}

	private void setAnimEnable(boolean enable) {
		animEnable = enable;
		boardView.setAnimEnable(animEnable);

		// Save the new setting to prefs.
		SharedPreferences prefs = getPreferences(0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putBoolean("animEnable", animEnable);
		editor.commit();

		selectAnimEnable();
	}

	// ******************************************************************** //
	// Game progress.
	// ******************************************************************** //

	/**
	 * This method is called each time the user clicks a cell.
	 */
	void cellClicked(Cell cell) {
		// Count the click, but only if this isn't a repeat click on the
		// same cell. This is because the tap interface only rotates
		// clockwise, and it's not fair to count an anti-clockwise
		// turn as 3 clicks.
		if (!isSolved && cell != prevClickedCell) {
			++clickCount;
			updateStatus();
			prevClickedCell = cell;
		}
	}

	// ******************************************************************** //
	// Game Control Functions.
	// ******************************************************************** //

	/**
	 * Wake up: the user has clicked the splash screen, so continue.
	 */
	private void wakeUp() {
		// If we are paused, just go to running. Otherwise (in the
		// welcome or game over screen), start a new game.
		if (gameState == State.PAUSED)
			setState(State.RUNNING, true);
		else
			startGame(null);
	}

	// Create a listener for the user starting the game.
	private final DialogInterface.OnClickListener startGameListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface arg0, int arg1) {
			setState(State.RUNNING, true);
		}
	};

	/**
	 * Start a game at a given skill level, or the previous skill level. The
	 * skill level chosen is saved to the preferences and becomes the default
	 * for next time.
	 * 
	 * @param sk
	 *            Skill level to start at; if null, use the previous skill from
	 *            the preferences.
	 */
	public void startGame(BoardView.Skill sk) {
		// Abort any existing game, so we know we're not just continuing.
		setState(State.ABORTED, false);

		// Sort out the previous and new skills. If we aren't
		// given a new skill, default to previous.
		BoardView.Skill prevSkill = gameSkill;
		gameSkill = sk != null ? sk : prevSkill;

		// Save the new skill setting in the prefs.
		SharedPreferences prefs = getPreferences(0);
		SharedPreferences.Editor editor = prefs.edit();
		editor.putString("skillLevel", gameSkill.toString());
		editor.commit();

		// Set the selected skill menu item to the current skill.
		selectCurrentSkill();
		statusMode.setText(gameSkill.label);

		// OK, now get going!
		Log.i(TAG, "startGame: " + gameSkill + " (was " + prevSkill + ")");

		// If we're going up to master or insane level, set up a message
		// to display the user to show him/her the new rules.
		int msg = 0;
		if (prevSkill != BoardView.Skill.INSANE) {
			if (gameSkill == BoardView.Skill.INSANE)
				msg = R.string.help_insane;
			else if (gameSkill == BoardView.Skill.MASTER
					&& prevSkill != BoardView.Skill.MASTER)
				msg = R.string.help_master;
		}

		// If we have a help message to show, show it; the dialog will
		// start the game (and hence the clock) when the user is ready.
		// Otherwise, start the game now.
		if (msg != 0)
			new AlertDialog.Builder(this).setMessage(msg)
					.setPositiveButton(R.string.button_ok, startGameListener)
					.show();
		else
			setState(State.RUNNING, true);
	}

	// ******************************************************************** //
	// Game State.
	// ******************************************************************** //

	/**
	 * Post a state change.
	 * 
	 * @param which
	 *            The state we want to go into.
	 */
	void postState(final State which) {
		stateHandler.sendEmptyMessage(which.ordinal());
	}

	private Handler stateHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			setState(State.getValue(m.what), true);
		}
	};

	/**
	 * Set the game state. Set the screen display and start/stop the clock as
	 * appropriate.
	 * 
	 * @param state
	 *            The state to go into.
	 * @param showSplash
	 *            If true, show the "pause" screen if appropriate. Otherwise
	 *            don't.
	 */
	private void setState(State state, boolean showSplash) {
		Log.i(TAG, "setState: " + state + " (was " + gameState + ")");

		// If we're not changing state, don't bother.
		if (state == gameState)
			return;

		// Save the previous state, and change.
		State prev = gameState;
		gameState = state;

		// Handle the state change.
		switch (gameState) {
		case NEW:
		case RESTORED:
			// Should never get these.
			break;
		case INIT:
			gameTimer.stop();
			if (showSplash)
				showSplashText(R.string.splash_text);
			break;
		case SOLVED:
			// This is a transient state, just used for signalling a win.
			gameTimer.stop();
			boardView.setSolved();

			// We allow the user to keep playing after it's over, but
			// don't keep reporting wins. Also don't brag or record a score
			// if the user used the solver.
			if (!isSolved && !solverUsed)
				reportWin(boardView.unconnectedCells());
			isSolved = true;

			// Keep running.
			gameState = State.RUNNING;
			break;
		case ABORTED:
			// Aborted is followed by something else,
			// so don't display anything.
			gameTimer.stop();
			break;
		case PAUSED:
			gameTimer.stop();
			if (showSplash)
				showSplashText(R.string.pause_text);
			break;
		case RUNNING:
			// Set us going, if this is a new game.
			if (prev != State.RESTORED && prev != State.PAUSED) {
				boardView.setupBoard(gameSkill);
				isSolved = false;
				clickCount = 0;
				prevClickedCell = null;
				solverUsed = false;
				gameTimer.reset();
				updateStatus();
				makeSound(Sound.START.soundId);
			}
			hideSplashText();
			if (!isSolved)
				gameTimer.start();
			break;
		}
	}

	// Create a listener for the user starting a new game.
	private final DialogInterface.OnClickListener newGameListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface arg0, int arg1) {
			startGame(null);
		}
	};

	/**
	 * Report that the user has won the game. Let the user continue to play with
	 * the layout, or start a new game.
	 * 
	 * @param unused
	 *            The number of unused cells. Normally zero, but it's sometimes
	 *            possible to solve the board without using all the cable bits.
	 */
	private void reportWin(int unused) {
		// Format the win message.
		long time = gameTimer.getTime();
		int titleId = R.string.win_title;
		String msg;

		if (unused != 0) {
			String fmt = appResources.getString(R.string.win_spares_text);
			msg = String.format(fmt, time / 60000, time / 1000 % 60,
					clickCount, unused);
		} else {
			String fmt = appResources.getString(R.string.win_text);
			msg = String
					.format(fmt, time / 60000, time / 1000 % 60, clickCount);
		}

		// See if we have a new high score.
		int ntiles = boardView.getBoardWidth() * boardView.getBoardHeight();
		String score = registerScore(gameSkill, ntiles, clickCount,
				(int) (time / 1000));
		if (score != null) {
			msg += "\n\n" + score;
			titleId = R.string.win_pbest_title;
		}

		// Display the dialog.
		String finish = appResources.getString(R.string.win_finish);
		msg += "\n\n" + finish;
		new AlertDialog.Builder(this).setTitle(titleId).setMessage(msg)
				.setPositiveButton(R.string.win_new, newGameListener)
				.setNegativeButton(R.string.win_continue, null).show();
	}

	// ******************************************************************** //
	// User Input.
	// ******************************************************************** //

	/**
	 * Called when the activity has detected the user's press of the back key.
	 * The default implementation simply finishes the current activity, but you
	 * can override this to do whatever you want.
	 * 
	 * Note: this is only called automatically on Android 2.0 on. On earlier
	 * versions, we call this ourselves from BoardView.onKeyDown().
	 */
	@Override
	public void onBackPressed() {
		// Go to the home screen. This causes our state to be saved, whereas
		// the default of finish() discards it.
		Intent homeIntent = new Intent();
		homeIntent.setAction(Intent.ACTION_MAIN);
		homeIntent.addCategory(Intent.CATEGORY_HOME);
		this.startActivity(homeIntent);
		return;
	}

	// ******************************************************************** //
	// Status Display.
	// ******************************************************************** //

	/**
	 * Update the status line to the current game state.
	 */
	void updateStatus() {
		// Use StringBuilders and a Formatter to avoid allocating new
		// String objects every time -- this function is called often!

		clicksText.setLength(3);
		clicksText.setCharAt(0, (char) ('0' + clickCount / 100 % 10));
		clicksText.setCharAt(1, (char) ('0' + clickCount / 10 % 10));
		clicksText.setCharAt(2, (char) ('0' + clickCount % 10));
		statusClicks.setText(clicksText);

		timeText.setLength(5);
		int time = (int) (gameTimer.getTime() / 1000);
		int min = time / 60;
		int sec = time % 60;
		timeText.setCharAt(0, (char) ('0' + min / 10));
		timeText.setCharAt(1, (char) ('0' + min % 10));
		timeText.setCharAt(2, ':');
		timeText.setCharAt(3, (char) ('0' + sec / 10));
		timeText.setCharAt(4, (char) ('0' + sec % 10));
		statusTime.setText(timeText);
	}

	/**
	 * Set the status text to the given text message. This hides the game board.
	 * 
	 * @param msgId
	 *            Resource ID of the message to set.
	 */
	private void showSplashText(int msgId) {
		splashText.setText(msgId);
		if (viewSwitcher.getDisplayedChild() != 1) {
			// Stop the game.
			boardView.surfaceStop();

			viewSwitcher.setInAnimation(animSlideInRight);
			viewSwitcher.setOutAnimation(animSlideOutRight);
			viewSwitcher.setDisplayedChild(1);
		}

		// Any key dismisses it, so we need focus.
		splashText.requestFocus();
	}

	/**
	 * Hide the status text, revealing the board.
	 */
	void hideSplashText() {
		if (viewSwitcher.getDisplayedChild() != 0) {
			viewSwitcher.setInAnimation(animSlideInLeft);
			viewSwitcher.setOutAnimation(animSlideOutLeft);
			viewSwitcher.setDisplayedChild(0);

			// Start the game -- after the animation.
			soundHandler.postDelayed(startRunner, ANIM_TIME);
		} else {
			// Make sure we're running -- we can get here after a restart.
			boardView.surfaceStart();
		}
	}

	private Runnable startRunner = new Runnable() {
		@Override
		public void run() {
			boardView.surfaceStart();
		}
	};

	// ******************************************************************** //
	// High Scores.
	// ******************************************************************** //

	/**
	 * Check to see if we need to register a new "high score" (personal best).
	 * 
	 * @param skill
	 *            The skill level of the completed puzzle.
	 * @param ntiles
	 *            The actual number of tiles in the board. This indicates the
	 *            actual difficulty level on the specific device.
	 * @param clicks
	 *            The user's click count.
	 * @param seconds
	 *            The user's time in SECONDS.
	 * @return Message to display to the user. Null if nothing to report.
	 */
	private String registerScore(BoardView.Skill skill, int ntiles, int clicks,
			int seconds) {
		// Get the names of the prefs for the counts for this skill level.
		String sizeName = "size" + skill.toString();
		String clickName = "clicks" + skill.toString();
		String timeName = "time" + skill.toString();

		// Get the best to date for this skill level.
		SharedPreferences scorePrefs = getSharedPreferences("scores",
				MODE_PRIVATE);
		int bestClicks = scorePrefs.getInt(clickName, -1);
		int bestTime = scorePrefs.getInt(timeName, -1);

		// See if we have a new best click count or time.
		long now = System.currentTimeMillis();
		SharedPreferences.Editor editor = scorePrefs.edit();
		String msg = null;
		if (clicks > 0 && (bestClicks < 0 || clicks < bestClicks)) {
			editor.putInt(sizeName, ntiles);
			editor.putInt(clickName, clicks);
			editor.putLong(clickName + "Date", now);
			msg = appResources.getString(R.string.best_clicks_text);
		}
		if (seconds > 0 && (bestTime < 0 || seconds < bestTime)) {
			editor.putInt(sizeName, ntiles);
			editor.putInt(timeName, seconds);
			editor.putLong(timeName + "Date", now);
			if (msg == null)
				msg = appResources.getString(R.string.best_time_text);
			else
				msg = appResources.getString(R.string.best_both_text);
		}

		if (msg != null)
			editor.commit();

		return msg;
	}

	// ******************************************************************** //
	// Sound.
	// ******************************************************************** //

	/**
	 * Create a SoundPool containing the app's sound effects.
	 */
	private SoundPool createSoundPool() {
		SoundPool pool = new SoundPool(3, AudioManager.STREAM_MUSIC, 0);
		for (Sound sound : Sound.values())
			sound.soundId = pool.load(this, sound.soundRes, 1);

		return pool;
	}

	/**
	 * Post a sound to be played on the main app thread.
	 * 
	 * @param which
	 *            ID of the sound to play.
	 */
	void postSound(final Sound which) {
		soundHandler.sendEmptyMessage(which.soundId);
	}

	private Handler soundHandler = new Handler() {
		@Override
		public void handleMessage(Message m) {
			makeSound(m.what);
		}
	};

	/**
	 * Make a sound.
	 * 
	 * @param soundId
	 *            ID of the sound to play.
	 */
	void makeSound(int soundId) {
		if (soundMode == SoundMode.NONE)
			return;

		float vol = 1.0f;
		if (soundMode == SoundMode.QUIET)
			vol = 0.3f;
		soundPool.play(soundId, vol, vol, 1, 0, 1f);
	}

	// ******************************************************************** //
	// State Save/Restore.
	// ******************************************************************** //

	/**
	 * Save game state so that the user does not lose anything if the game
	 * process is killed while we are in the background.
	 * 
	 * @param outState
	 *            A Bundle in which to place any state information we wish to
	 *            save.
	 */
	private void saveState(Bundle outState) {
		// Save the skill level and game state.
		outState.putString("gameSkill", gameSkill.toString());
		outState.putString("gameState", gameState.toString());
		outState.putBoolean("isSolved", isSolved);

		// Save the game state of the board.
		boardView.saveState(outState);

		// Restore the game timer and click count.
		gameTimer.saveState(outState);
		outState.putInt("clickCount", clickCount);
		outState.putBoolean("solverUsed", solverUsed);
	}

	/**
	 * Restore our game state from the given Bundle.
	 * 
	 * @param map
	 *            A Bundle containing the saved state.
	 * @return true if the state was restored OK; false if the saved state was
	 *         incompatible with the current configuration.
	 */
	private boolean restoreState(Bundle map) {
		// Get the skill level and game state.
		gameSkill = Skill.valueOf(map.getString("gameSkill"));
		gameState = State.valueOf(map.getString("gameState"));
		isSolved = map.getBoolean("isSolved");

		// Restore the state of the game board.
		boolean restored = boardView.restoreState(map, gameSkill);

		// Restore the game timer and click count.
		if (restored) {
			restored = gameTimer.restoreState(map, false);
			clickCount = map.getInt("clickCount");
			solverUsed = map.getBoolean("solverUsed");
		}

		return restored;
	}

	// ******************************************************************** //
	// Private Types.
	// ******************************************************************** //

	// This class implements the game clock. All it does is update the
	// status each tick.
	private final class GameTimer extends Timer {

		GameTimer() {
			// Tick every 0.25 s.
			super(250);
		}

		@Override
		protected boolean step(int count, long time) {
			updateStatus();

			// Run until explicitly stopped.
			return false;
		}

	}

	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	private static final String TAG = "netscramble";

	// Time in ms for slide in/out animations.
	private static final int ANIM_TIME = 500;

	// ******************************************************************** //
	// Member Data.
	// ******************************************************************** //

	// The app's resources.
	private Resources appResources;

	// The game board.
	private BoardView boardView = null;

	// The status bar, consisting of 3 status fields.
	private TextView statusClicks;
	private TextView statusMode;
	private TextView statusTime;

	// Text buffers used to format the click count and time. We allocate
	// these here, so we don't allocate new String objects every time
	// we update the status -- which is very often.
	private StringBuilder clicksText;
	private StringBuilder timeText;

	// Sound pool used for sound effects.
	private SoundPool soundPool;

	// The text widget used to display status messages. When visible,
	// it covers the board.
	private TextView splashText = null;

	// View switcher used to switch between the splash text and
	// board view.
	private ViewAnimator viewSwitcher = null;

	// Animations for the view switcher.
	private TranslateAnimation animSlideInLeft;
	private TranslateAnimation animSlideOutLeft;
	private TranslateAnimation animSlideInRight;
	private TranslateAnimation animSlideOutRight;

	// The menu used to select the skill level. We keep this so we can
	// set the selected item.
	private Menu mainMenu;

	// The state of the current game.
	private State gameState;

	// When gameState == State.RESTORED, this is our restored game state.
	private State restoredGameState;

	// Flag whether the board has been solved. Once solved, the user
	// can keep playing, but we don't count score any more.
	private boolean isSolved;

	// The currently selected skill level.
	private BoardView.Skill gameSkill;

	// Timer used to time the game.
	private GameTimer gameTimer;

	// Current sound mode.
	private SoundMode soundMode;

	// True to enable the network animation.
	private boolean animEnable;

	// Number of times the user has clicked.
	private int clickCount = 0;

	// The previous cell that was clicked. Used to detect multiple clicks
	// on the same cell.
	private Cell prevClickedCell = null;

	// Flag if the user has invoked the auto-solver for this game.
	private boolean solverUsed = false;

}
