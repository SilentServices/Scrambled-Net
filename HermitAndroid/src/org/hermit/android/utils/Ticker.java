
/**
 * org.hermit.android.utils: useful Android utilities.
 * 
 * These classes provide various Android-specific utilities.
 *
 * <br>Copyright 2011 Ian Cameron Smith
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


package org.hermit.android.utils;


import java.util.ArrayList;
import java.util.Calendar;

import android.os.Handler;
import android.util.Log;


/**
 * Scheduler for regular callbacks to multiple clients.  This class can
 * be used to generate callbacks to several listeners, each with their
 * own desired callback interval, all from a single thread.
 * 
 * <p>Callbacks are all based on a regular number of seconds within a day,
 * and are delivered as closely as possible to the interval boundary.
 * For example, if a client asks for a tick every two hours, it will get
 * ticks as close as possible to the even hours of the day.</p>
 * 
 * <p>The Ticker can be stopped and started multiple times.</p>
 *
 * @author Ian Cameron Smith
 */
public class Ticker {

	// ******************************************************************** //
	// Public Types.
	// ******************************************************************** //

	/**
	 * A listener that callers can use to be notified of ticks, along with
	 * some potentially useful info.  You can use this as an alternative
	 * to using a Handler.
	 */
	public static abstract class Listener {
		/**
		 * This method is called to notify the client of a tick.
		 * 
		 * @param	time		Current system time in ms.
		 * @param	daySecs		Number of seconds into the day that this
		 * 						tick relates to.
		 */
		public abstract void tick(long time, int daySecs);
		
		private long time;
		private int daySecs;
	}
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Create a ticker.  To use it, call {@link #listen(int, Handler)} to
	 * set one or more listeners; you must also call {@link #start()} to
	 * set it running.
	 */
	public Ticker() {
		// Get the time in milliseconds of the start of the day.
		// This is used for aligning to the configured interval.
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(System.currentTimeMillis());
		cal.set(Calendar.MILLISECOND, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.HOUR_OF_DAY, 0);
		dayStart = cal.getTimeInMillis();

		// Create the listeners list.
		clients = new ArrayList<ClientData>();
		
		// Set a default interval (so we're always somewhat warm).
		tickSecs = 3600;
	}

	
	// ******************************************************************** //
	// Run Control.
	// ******************************************************************** //

	/**
	 * Start this ticker.  All callbacks that were previously registered will
	 * still be in force.  To stop this Ticker, call {@link #stop()}.
	 */
	public void start() {
		// Make sure we're not currently running.
		stop();
		
		synchronized (this) {
			// Re-calculate our tick interval.
			tickSecs = calculateInterval();
	    	if (DBG) Log.i(TAG, "start(): tick=" + tickSecs);

			enable = true;
			tickerThread = new Thread(tickRunner);
			tickerThread.start();
		}
	}


	/**
	 * Stop this ticker.  All callbacks will be remembered, so if you start
	 * the Ticker again (using {@link #start()}), they will begin getting
	 * notified again.
	 */
	public void stop() {
		synchronized (this) {
			enable = false;
			if (tickerThread != null) {
				if (DBG) Log.i(TAG, "stop()");
				tickerThread.interrupt();
				tickerThread = null;
			}
		}
	}


	// ******************************************************************** //
	// Listening.
	// ******************************************************************** //

	/**
	 * Schedule a Listener to get callbacks at a regular interval,
	 * as closely as possible.
	 * 
	 * <p>Callbacks will be delivered as nearly as possible on the specified
	 * tick interval, relative to the start of the day. Examples:
	 * 
	 * <ul>
	 * <li>if a client asks for a tick every two hours, it will get
	 *     ticks as close as possible to the even hours of the day.</li>
	 * <li>if a client asks for a tick every seven hours, it will get
	 *     ticks as close as possible to 0:00, 7:00, 14:00, and 21:00;
	 *     then starting over again at 0:00 the next day.</li>
	 * </ul></p>
	 * 
	 * <p>Note that the Ticker must be started with {@link #start()} in
	 * order for the listeners to be called.</p>
	 * 
	 * @param	secs		Callback interval, in seconds.
	 * @param	l			Listener to invoke, on the caller's thread,
	 * 						on the interval.
	 */
	public void listen(int secs, Listener l) {
		listen(secs, new ClientData(secs, new Handler(), l));
	}


	/**
	 * Schedule a Handler to get messages at a regular interval,
	 * as closely as possible.
	 * 
	 * <p>Messages will be delivered as nearly as possible on the specified
	 * tick interval, relative to the start of the day. Examples:
	 * 
	 * <ul>
	 * <li>if a client asks for a tick every two hours, it will get
	 *     ticks as close as possible to the even hours of the day.</li>
	 * <li>if a client asks for a tick every seven hours, it will get
	 *     ticks as close as possible to 0:00, 7:00, 14:00, and 21:00;
	 *     then starting over again at 0:00 the next day.</li>
	 * </ul></p>
	 * 
	 * <p>Note that the Ticker must be started with {@link #start()} in
	 * order for the listeners to be called.</p>
	 * 
	 * @param	secs		Callback interval, in seconds.
	 * @param	handler		Handler to send (empty) callback messages to
	 * 						on the interval.
	 */
	public void listen(int secs, Handler handler) {
		listen(secs, new ClientData(secs, handler, null));
	}
	
	
	/**
	 * Schedule the given listener to get callbacks at a regular interval,
	 * as closely as possible.
	 *
	 * @param	secs		Callback interval, in seconds.
	 * @param	l			Listener to register.
	 */
	private void listen(int secs, ClientData l) {
		synchronized (this) {
			clients.add(l);

			// Set the overall interval.
			if (clients.size() == 1)
				tickSecs = secs;
			else
				tickSecs = gcd(tickSecs, secs);
		}
		if (DBG) Log.i(TAG, "listen(" + secs + "): tick=" + tickSecs);

		// Interrupt the ticker thread, so it can re-schedule.
		if (tickerThread != null)
			tickerThread.interrupt();
	}


	/**
	 * De-register a given handler.  All callbacks to this handler will
	 * be removed.
	 * 
	 * @param	l			Runnable to unregister.
	 */
	public void unlisten(Listener l) {
		synchronized (this) {
			// Remove all listeners with this handler.
			for (int i = 0; i < clients.size(); ) {
				ClientData c = clients.get(i);
				if (c.listener == l)
					clients.remove(i);
				else
					++i;
			}

			// Set the new tick interval.
			tickSecs = calculateInterval();
		}
		if (DBG) Log.i(TAG, "unlisten(): tick=" + tickSecs);

		// Interrupt the ticker thread, so it can re-schedule.
		if (tickerThread != null)
			tickerThread.interrupt();
	}


	/**
	 * De-register a given handler.  All callbacks to this handler will
	 * be removed.
	 * 
	 * @param	handler		Handler to unregister.
	 */
	public void unlisten(Handler handler) {
		synchronized (this) {
			// Remove all listeners with this handler.
			for (int i = 0; i < clients.size(); ) {
				ClientData c = clients.get(i);
				if (c.handler == handler)
					clients.remove(i);
				else
					++i;
			}

			// Set the new tick interval.
			tickSecs = calculateInterval();
		}
		if (DBG) Log.i(TAG, "unlisten(): tick=" + tickSecs);

		// Interrupt the ticker thread, so it can re-schedule.
		if (tickerThread != null)
			tickerThread.interrupt();
	}

	
	// ******************************************************************** //
	// Implementation.
	// ******************************************************************** //

	/**
	 * Determine whether we are still running.
	 * 
	 * @return				true iff we're still enabled.
	 */
	private boolean isEnabled() {
		boolean res;
		synchronized (this) {
			res = enable;
		}
		return res;
	}

	
	/**
	 * Re-calculate our tick interval by calculating the gcd of
	 * all the tick intervals of the remaining listeners.
	 * 
	 * @return				Tick interval which matches all the listeners.
	 */
	private int calculateInterval() {
		int size = clients.size();
		if (size == 0)
			return 3600;
		else {
			int iv = clients.get(0).interval;
			for (int i = 1; i < size; ++i)
				iv = gcd(iv, clients.get(i).interval);
			return iv;
		}
	}

	
	/**
	 * Run this Ticker.  Terminates when we're killed.
	 */
	private Runnable tickRunner = new Runnable() {
		@Override
		public void run() {
			long dayTime;
			while (isEnabled()) {
				try {
					// Try to sleep up to the next interval boundary, so we
					// tick just about on the interval boundary.
					long tickMillis = tickSecs * 1000;
					dayTime = System.currentTimeMillis() - dayStart;
					Thread.sleep(tickMillis - dayTime % tickMillis);
					if (!isEnabled())
						break;

					// OK, now figure out what second boundary we're on.  We
					// calculate the seconds from some day boundary.
					Long time = System.currentTimeMillis();
					dayTime = time - dayStart;
					int daySec = (int) ((dayTime + 250) / 1000) % DAY_SECS;

					// Tell every listener whose tick interval we hit.
					synchronized (this) {
						for (ClientData c : clients) {
							if (daySec % c.interval == 0)
								c.tick(time, daySec);
						}
					}
				} catch (InterruptedException e) {
					// Do nothing.  Interrupts are used to wake us from
					// sleep and re-check the configuration.
				}
			}
		}
	};


	/**
	 * Calculate the GCD of two integers.
	 * 
	 * @param	a			First integer.
	 * @param	b			Second integer.
	 * @return				gcd(a, b).
	 */
	private static final int gcd(int a, int b) {
		if (b == 0)
			return a;
		else
			return gcd(b, a % b);
	}


	// ******************************************************************** //
	// Private Types.
	// ******************************************************************** //

	// Encapsulate a listener (represented by a Handler) with its
	// requested notification interval.  If a Listener is also present,
	// it is the callback to invoke; otherwise we just send a message to
	// the handler.  The Handler must be supplied, as it is the handle
	// on the thread we need to notify.
	private static final class ClientData {
		ClientData(int i, Handler h, Listener l) {
			interval = i;
			handler = h;
			listener = l;

			if (listener != null) {
				poster = new Runnable() {
					@Override
					public void run() {
						listener.tick(listener.time, listener.daySecs);
					}
				};
			}
		}

		void tick(long time, int daySecs) {
			if (listener != null) {
				listener.time = time;
				listener.daySecs = daySecs;
				handler.post(poster);
			} else
				handler.sendEmptyMessage(1);
		}

		final int interval;
		final Handler handler;
		final Listener listener;

		Runnable poster = null;
	}
	
	
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

	// Debugging tag.
	private static final String TAG = "Ticker";

	// Seconds in a day.
	private static final int DAY_SECS = 24 * 3600;
	
	private static final boolean DBG = false;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// Thread running this ticker.
	private Thread tickerThread = null;
	
	// True iff the ticker is enabled.  Set to false to stop it.
	private boolean enable;

	// The time in milliseconds of the start of the day.
	private long dayStart;
	
	// The registered listeners.
	private ArrayList<ClientData> clients;

	// The internal ticker tick interval, in seconds.
	private int tickSecs;

}

