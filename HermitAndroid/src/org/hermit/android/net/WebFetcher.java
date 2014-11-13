
/**
 * org.hermit.android.net: Android utilities for accessing network data.
 * 
 * These classes provide some basic utilities for accessing and cacheing
 * various forms of data from network servers.
 *
 * <br>Copyright 2009 Ian Cameron Smith
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


package org.hermit.android.net;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.LinkedList;

import android.os.Process;
import android.util.Log;


/**
 * This class fetches data from the web without blocking the main app.
 */
public abstract class WebFetcher
	extends Thread
{

    // ******************************************************************** //
    // Public Types.
    // ******************************************************************** //
	
	/**
	 * Web fetching exception.  Used to signal a problem while fetching data.
	 */
	public static class FetchException extends Exception {
	    /**
	     * Create a FetchException with a message.
	     * @param s    The exception message.
	     */
		public FetchException(String s) {
			super("Web fetch error: " + s);
		}
        /**
         * Create a FetchException based on another exception.
         * @param s    The exception message.
         * @param e    The root exception.
         */
		public FetchException(String s, Exception e) {
			super("Web fetch error: " + s +
				(e.getMessage() == null ? "" : " (" + e.getMessage() + ")"), e);
		}
		private static final long serialVersionUID = 4699577452411347104L;
	}


	// ******************************************************************** //
	// Public Classes.
	// ******************************************************************** //
	
	/**
	 * Listener for incoming web data.
	 */
	public static interface Listener {	
		/**
		 * This method is invoked when a data item is retrieved from
		 * one of the URLs we were invoked on.
		 * 
		 * @param	url				The URL that this record came from.
		 * @param	obj				The object that was loaded; the type
		 * 							depends on the fetcher class used.
		 * @param	date			The last modified time of the source file,
		 * 							as reported by the server, in ms UTC.
		 */
		public void onWebData(URL url, Object obj, long date);
		
		/**
		 * This method is invoked when the URLs have been fully fetched.
		 */
		public void onWebDone();
		
		/**
		 * This method is invoked if an error occurs when fetching web data.
		 * 
		 * @param	msg				The error message.
		 */
		public void onWebError(String msg);
	}
	

	// ******************************************************************** //
	// Fetch Queue.
	// ******************************************************************** //

	/**
	 * Queue a web fetch.  It will be executed when the current fetches
	 * are done.
	 * 
	 * @param   fetcher        The web fetcher to queue.
	 */
	public static void queue(WebFetcher fetcher) {
		synchronized (fetchQueue) {
			if (queueRunner == null)
				queueRunner = new Runner();
			fetchQueue.addLast(fetcher);
			fetchQueue.notify();
		}
	}


	/**
	 * Stop all fetch operations.
	 */
	public static void killAll() {
		// Stop the queue.
		synchronized (fetchQueue) {
			if (queueRunner != null)
				queueRunner.interrupt();
			fetchQueue.clear();
		}
		
		// Stop all extant fetchers.
		synchronized (allFetchers) {
			for (WebFetcher f : allFetchers)
				f.kill();
		}
	}
	
	
	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
     * Create a fetcher to get data from a given URL.  Data will
     * be fetched asynchronously from the URL, and passed as
     * it arrives to the given client.
     *
	 * You can start the fetch when you want by calling start(); or you can
	 * pass it to queue() to be executed when other fetches are done.
	 * 
	 * @param	url				The URL to fetch data from.
	 * @param	client			Client to pass the data to.
	 * @param	timeout			Maximum time in ms for which the job will be
	 * 							allowed to run.
	 */
	public WebFetcher(URL url, Listener client, long timeout) {
		this(new URL[] { url }, client, timeout, 0);
	}
	

    /**
     * Create a fetcher to get data from a given URL.  Data will
     * be fetched asynchronously from the URL, and passed as
     * it arrives to the given client.
     * 
     * You can start the fetch when you want by calling start(); or you can
     * pass it to queue() to be executed when other fetches are done.
     * 
     * @param   url             The URL to fetch data from.
     * @param   client          Client to pass the data to.
     * @param   timeout         Maximum time in ms for which the job will be
     *                          allowed to run.
     * @param   newer           If-modified-since time in ms UTC.  The
     *                          fetch will only be carried out if the remote
     *                          resource has been modified since this time.
     *                          If zero, fetch without this condition.
     */
    public WebFetcher(URL url, Listener client, long timeout, long newer) {
        this(new URL[] { url }, client, timeout, newer);
    }
    

	/**
	 * Create a fetcher to get data from a given list of URLs.  Data will
	 * be fetched asynchronously from each URL in sequence, and passed as
	 * it arrives to the given client.  If any URL fails, the whole fetch
	 * will be aborted.  If all URLs succeed, onWebDone() will be called
	 * in the listener.
	 * 
     * You can start the fetch when you want by calling start(); or you can
     * pass it to queue() to be executed when other fetches are done.
	 * 
     * @param   urls            The URLs to fetch data from.
	 * @param	client			Client to pass the data to.
	 * @param	timeout			Maximum time in ms for which the job will be
	 * 							allowed to run.
	 * @param	newer			If-modified-since time in ms UTC.  The
	 * 							fetch will only be carried out if the remote
	 * 							resource has been modified since this time.
	 * 							If zero, fetch without this condition.
	 */
	public WebFetcher(URL[] urls, Listener client, long timeout, long newer) {
		dataUrls = urls;
		dataClient = client;
		this.timeout = timeout;
		newerThanDate = newer;
		
		// Register on the list of all fetchers.
		synchronized (allFetchers) {
			allFetchers.add(this);
		}
	}
	

	// ******************************************************************** //
	// Thread control.
	// ******************************************************************** //

	/**
	 * Kill this fetcher.  Don't start it, don't do any more callbacks.
	 */
	public void kill() {
		synchronized (this) {
			killed = true;
			if (isAlive())
				interrupt();
		}
	}
	
	
	/**
	 * Thread's main method.  Just fetch the URLs in sequence.  If one
	 * fails, stop there.
	 */
	@Override
	public void run() {
		synchronized (this) {
			if (killed)
				return;
		}
			
		// Run this fetch.
	    URL current = null;
		try {
		    // Fetch each URL in sequence.  If one fails, stop there.
		    for (URL url : dataUrls) {
	            // Sleep a wee bit, to ensure our client gets some running in first.
	            sleep(500);
	            if (isInterrupted())
	                throw new InterruptedException();
	        
	    		current = url;
		        Log.i(TAG, "R: start " + url);
		        fetch(url, newerThanDate);
		        
	    		if (isInterrupted())
	    			throw new InterruptedException();
		    }

			// Tell the client that all URLs are done.
			Log.i(TAG, "R: finished all " + current);
			dataClient.onWebDone();
		} catch (IOException e) {
			Log.e(TAG, "R: IOException: " + current);
			String msg = e.getClass().getName() + ": " + e.getMessage();
			dataClient.onWebError(msg);
		} catch (FetchException e) {
			Log.e(TAG, "R: FetchException: " + current);
			String msg = e.getClass().getName() + ": " + e.getMessage();
			dataClient.onWebError(msg);
		} catch (InterruptedException e) {
            Log.e(TAG, "R: InterruptedException: " + current);
            String msg = "Interrupted";
            dataClient.onWebError(msg);
        } finally {
    		// We're done; de-register from the list of all fetchers.
    		synchronized (allFetchers) {
    			allFetchers.remove(this);
    		}
       }
	}
	
	
	// ******************************************************************** //
	// Data Fetching.
	// ******************************************************************** //

	/**
	 * Fetch an object from the given URL.
	 * 
	 * @param	url				The URL to fetch.
	 * @param	newer			If-modified-since time in ms UTC.  The
	 * 							fetch will only be carried out if the remote
	 * 							resource has been modified since this time.
	 * 							If zero, fetch without this condition.
     * @throws  FetchException  Some problem was detected, such as a timeout.
     * @throws  IOException     An I/O error occurred.
	 */
    protected void fetch(URL url, long newer)
    	throws FetchException, IOException
    {
    	// Attempt to connect to the URL.  If we fail, just bomb out.
		InputStream stream = null;
		try {
			// Set up the connection parameters.
			URLConnection conn = url.openConnection();
			if (newer != 0)
				conn.setIfModifiedSince(newer);
    		
    		// If we were killed, bomb out.
    		if (isInterrupted())
    			throw new FetchException("timed out");

			// Open the connection.
			conn.connect();
    		stream = conn.getInputStream();
    		
    		// If we were killed, bomb out.
    		if (isInterrupted())
    			throw new FetchException("timed out");

    		// Fetch from the stream.
    		handle(url, conn, stream);
    		
    		// If we were killed, bomb out.
    		if (isInterrupted())
    			throw new FetchException("timed out");
		} finally {
    		// Clean up the streams etc.  Other than that, we just bomb.
    		try {
    			if (stream != null)
    				stream.close();
    		} catch (IOException e1) { }
    	}
    }
	

	/**
	 * Handle data from the given stream.
	 * 
	 * @param	url				The URL we're reading.
	 * @param	conn			The current connection to the URL.
	 * @param	stream			The InputStream to read from.
     * @throws  FetchException  Some problem was detected, such as a timeout.
     * @throws  IOException     An I/O error occurred.
	 */
    protected void handle(URL url, URLConnection conn, InputStream stream)
        throws FetchException, IOException
    {
		InputStreamReader reads = null;
		BufferedReader readc = null;
		try {
			// Open a buffered reader on the connection.  What a chore...
    		reads = new InputStreamReader(stream);
    		readc = new BufferedReader(reads, 4096);
    		 
    		// Get the input.
    		handle(url, conn, readc);
		} finally {
    		// Clean up the streams etc.  Other than that, we just bomb.
    		try {
    			if (readc != null)
    				readc.close();
    		} catch (IOException e1) { }
    		try {
    			if (reads != null)
    				reads.close();
    		} catch (IOException e1) { }
    	}
    }
	

	/**
	 * Handle data from the given BufferedReader.
	 * 
	 * @param	url				The URL we're reading.
	 * @param	conn			The current connection to the URL.
	 * @param	stream			The BufferedReader to read from.
     * @throws  FetchException  Some problem was detected, such as a timeout.
     * @throws  IOException     An I/O error occurred.
	 */
    protected void handle(URL url, URLConnection conn, BufferedReader stream)
        throws FetchException, IOException
    {
    }
    

	// ******************************************************************** //
	// Private Class.
	// ******************************************************************** //
	
    /**
     * This class runs the queue of fetch operations, if we're using
     * a queue.  It also takes care of timeouts on queue items.
     */
    private static final class Runner
    	extends Thread
    {
    	Runner() {
    		start();
    	}

    	@Override
		public void run() {
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND);

    		while (true) {
	    		if (isInterrupted())
	    			break;

	    		WebFetcher job;
        		
        		// Get the next job from the queue.  If there isn't one,
        		// wait until there is.
    			synchronized (fetchQueue) {
    				job = fetchQueue.poll();
    				if (job == null) {
    					try {
							fetchQueue.wait();
						} catch (InterruptedException e) { }
			    		if (isInterrupted())
			    			break;
    					continue;
    				}
    			}
	    		if (isInterrupted())
	    			break;
    			
    			// Kick off the job.
    			job.start();
    			
    			// Now wait for it to finish, but only up to the timeout.
    			try {
    				job.join(job.timeout);
    			} catch (InterruptedException e) { }
	    		if (isInterrupted())
	    			break;
    			
    			// If it didn't finish, kill it.
    			if (job.isAlive())
    				job.interrupt();
    		}
    	}
    }
    
    
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "WebFetcher";
	
	// List of all WebFetchers in operation.
	private static ArrayList<WebFetcher> allFetchers =
										new ArrayList<WebFetcher>();

    // This is the queue of items to be fetched.
    private static final LinkedList<WebFetcher> fetchQueue =
    									new LinkedList<WebFetcher>();
       
    // Queue runner.
    private static Runner queueRunner = null;
    

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The timeout in ms for this fetcher to complete.
    protected final long timeout;

	// Set to true to kill this thread.
    protected boolean killed = false;
	
	// The URLs we're fetching.
    protected final URL[] dataUrls;
	
    // If this is zero, it has no effect.  If non-zero, it is taken as a
    // time in ms UTC; the remote resource will only be fetched if it has
    // been modified since that time.
    protected final long newerThanDate;
    
	// The client to pass the data to.
	protected final Listener dataClient;
	
}

