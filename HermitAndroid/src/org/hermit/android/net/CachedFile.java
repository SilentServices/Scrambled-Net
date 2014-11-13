
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


import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Observable;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


/**
 * This class manages a set of web-based files and caches them locally.
 * Data about the files is held in a local database.
 */
public class CachedFile
	extends Observable
	implements WebFetcher.Listener
{

	// ******************************************************************** //
	// Public Classes.
	// ******************************************************************** //
	
	/**
	 * Class describing an entry in the cache; i.e. a cached file.
	 */
	public static final class Entry {
		/**
		 * Create an entry.
		 * 
		 * @param	url				The URL of the file that was loaded.
		 * @param	name			The name of the file.
		 */
		private Entry(URL url, String name) {
			this.url = url;
			this.name = name;
		}
		
		/**
		 * The URL of the file that was loaded.
		 */
		public final URL url;
		
		/**
		 * The local name of the file.
		 */
		public String name;
		
		/**
		 * The path of the local copy of the file.
		 */
		public File path = null;
		
		/**
		 * The last modified time of the file, as reported by the server,
         * in ms UTC.
		 */
		public long date = 0;
	}
	

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a cache of web-based files.  This is private, as only one
	 * instance exists.
	 * 
	 * @param	name			The name of this source, and its database table.
	 * @param	urls			The list of URLs to cache.
	 */
	public CachedFile(String name, URL[] urls) {
		sourceName = name;
		
		targetFiles = new HashMap<URL, Entry>();
		
		// For each URL, make up an initial entry for it, containing
		// the URL and a suitable local filename.
		for (URL url : urls) {
			String fn = url.getPath().substring(1);
			fn = fn.replace('/', '-');
			Entry file = new Entry(url, fn);
    		Log.i(TAG, "Caching file: " + url.getPath() + " -> " + fn);
			targetFiles.put(url, file);
		}
	}
	
	
	// ******************************************************************** //
	// Database Management.
	// ******************************************************************** //

	/**
	 * Create our table in the database.
     * 
     * @param	db				The database.
	 */
    public void createTable(SQLiteDatabase db) {
    	// Create our data table.
		String create = "CREATE TABLE " + sourceName + " (" +
		  				"url TEXT PRIMARY KEY UNIQUE ON CONFLICT REPLACE";
		create += ", name TEXT";
		create += ", path TEXT";
		create += ", date INTEGER";
		create += ");";
		db.execSQL(create);
    }


    /**
     * Upgrade or table in the database to a new version.
     * 
     * @param	db				The database.
     * @param	oldV			Version we're upgrading from.
     * @param	newV			Version we're upgrading to.
     */
    public void upgradeTable(SQLiteDatabase db, int oldV, int newV) {
        // We can simply dump all the old data, since the DB is just
    	// a cache of web data.
    	db.execSQL("DROP TABLE IF EXISTS " + sourceName);
    	createTable(db);
    }


	/**
	 * Set the database we use for storing our data.
     * 
     * @param	db			The database.  Will be null if the database is
     *                      being closed.
	 */
    public void setDatabase(SQLiteDatabase db) {
        synchronized (this) {
            database = db;

            // If we have a database, make sure all the URLs are in it.
            if (database != null)
                for (Entry file : targetFiles.values())
                    syncDatabase(file);
        }
    }


	/**
	 * Sync the database info for the given file with the given Entry.
     * If the target is missing from the database, add it with path=null
     * indicating the file is not loaded.  If we have a file that is already
     * loaded, notify the observers that it's here.
	 * 
	 * @param	file		An Entry which contains our current guess
	 * 						at the file's state.
	 */
    private void syncDatabase(Entry file) {
    	// Get the database info on the file.
    	Cursor c = database.query(sourceName, null,
    							  "url=\'" + file.url.toString() + "\'",
    							  null, null, null, null, null);

    	// If there's no record for the file, add one.  Don't set the path.
    	// If there is a record, load it into file.
    	if (!c.moveToFirst()) {
    		Log.i(TAG, "New file: " + file.url.toString());
        	ContentValues vals = new ContentValues();
    		vals.put("url", file.url.toString());
    		vals.put("name", file.name);
    		vals.put("date", 0);
    		database.insert(sourceName, "path", vals);
    	} else {
    		// If the database has a different local name, use that.
    		int nameIndex = c.getColumnIndex("name");
    		String n = c.getString(nameIndex);
    		if (n != null)
    			file.name = n;

    		// Get the path where it's stored, if any.
    		int pathIndex = c.getColumnIndex("path");
    		String p = c.getString(pathIndex);
    		
    		// If it's loaded, fill in our copy of the data and notify
    		// all clients that we have the file.
    		if (p != null) {
        		Log.i(TAG, "Existing file loaded: " + file.url.toString());
    			file.path = new File(p);

    			// Get the date loaded, if any.
    			int dateIndex = c.getColumnIndex("date");
    			Long d = c.getLong(dateIndex);
    			file.date = d != null ? d : 0;
    			
    			// Notify the clients.  Note that if one client calls
    			// invalidate(), the others will see an entry with no data.
    			setChanged();
    			notifyObservers(file.url);
    		} else {
        		Log.i(TAG, "Existing file not here: " + file.url.toString());
    			file.path = null;
    			file.date = 0;
    		}

    	}

    	c.close();
    }


	// ******************************************************************** //
	// Data Access.
	// ******************************************************************** //

    /**
     * Query for a given file in the cache.
     * 
	 * @param	url			The URL of the file we want.
     * @return				null if the URL is not being cached at all.
     * 						Otherwise, an Entry containing info on the file;
     * 						path will be non-null if the file has been loaded,
     * 						and date will be its reported last mod time.
     */
    public synchronized Entry getFile(URL url) {
		return targetFiles.get(url);
    }
    
    
    /**
     * Invalidate the given file in the cache (perhaps it was corrupted).
     * 
     * @param	url			The URL of the file to invalidate.
     */
    public synchronized void invalidate(URL url) {
    	// Get our local record.
    	Entry entry = targetFiles.get(url);
    	if (entry == null)
    		return;
    	
    	// Clear it out.
    	entry.path = null;
    	entry.date = 0;
    	
    	// Write the change to the database.
    	if (database != null) {
    	    ContentValues vals = new ContentValues();
    	    vals.put("url", url.toString());
    	    vals.put("name", entry.name);
    	    database.replace(sourceName, "path", vals);
    	}
	}
    
    
	// ******************************************************************** //
	// Update.
	// ******************************************************************** //

	/**
	 * Check to see whether we need to update our cached copies of the
	 * files.  If so, kick off a web fetch.
	 * 
	 * The observers will be notified for each file that we load.
	 * 
	 * @param	context			The application context.  Used for
	 * 							determining where the local files are kept.
	 * @param	now				The current time in millis.
	 */
    public synchronized void update(Context context, long now) {
    	if (database == null)
    		return;
		
		// If it's too soon since the last check, don't even try.
		if (now - lastDataCheck < MIN_CHECK_INTERVAL)
			return;
		
		// Check all the files we're caching, and see if any of them
		// is absent or old enough to need refreshing.  If so, kick
		// off a load.
		boolean checking = false;
		for (Entry entry : targetFiles.values()) {
			if (entry.path == null || now - entry.date > REFRESH_INTERVAL) {
				FileFetcher ff =
					new FileFetcher(context, entry.url, entry.name,
									this, FETCH_TIMEOUT, entry.date);
				WebFetcher.queue(ff);
				checking = true;
			}
		}

		if (checking)
			lastDataCheck = now;
    }


    // ******************************************************************** //
    // Fetched Data Handling.
    // ******************************************************************** //

	/**
	 * This method is invoked when a data item is retrieved from
	 * the URL we were invoked on.
	 * 
	 * @param	url				The URL of the source being loaded.
	 * @param	obj				The object that was loaded; the type
	 * 							depends on the fetcher class used.
	 * @param	date			The last modified time of the source file,
	 * 							as reported by the server, in ms UTC.
	 */
	@Override
	public void onWebData(URL url, Object obj, long date) {
	    if (database == null)
	        return;
	    
		if (!(obj instanceof File)) {
			onWebError("Loaded object for " + url + " not a File!");
			return;
		}
		File path = (File) obj;
		Log.i(TAG, "Loaded " + url.toString());

		// Add it to the database with its new date.
		synchronized (this) {
		    ContentValues vals = new ContentValues();
		    vals.put("url", url.toString());
		    vals.put("name", path.getName());
		    vals.put("path", path.getPath());
		    vals.put("date", date);
		    database.replace(sourceName, "path", vals);

		    // Update the Entry as well.
		    Entry file = targetFiles.get(url);
		    file.path = path;
		    file.date = date;
		}

		// Inform the clients that we have data.  Note that if one client
		// calls invalidate(), the others will see an entry with no data.
		setChanged();
		notifyObservers(url);
	}

	
	/**
	 * This method is invoked when the given URL has been fully fetched.
	 */
    @Override
	public void onWebDone() {
	}

	
	/**
	 * Handle an error while fetching web data.
	 * 
	 * @param	msg				The error message.
	 */
	@Override
	public synchronized void onWebError(String msg) {
		Log.e(TAG, msg);
	}


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "CachedFile";

	// Minimum interval in ms between checks for file updates.
    private static final long MIN_CHECK_INTERVAL = 60 * 60 * 1000;
	
	// The age of a file in ms at which we will look for a newer version.
	private static final long REFRESH_INTERVAL = 240 * 60 * 1000;
	
	// The timeout in ms for fetching a file.
	private static final long FETCH_TIMEOUT = 15 * 60 * 1000;
	

	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// The name of this source, and its database table.
	private final String sourceName;
	
	// The files to cache.
	private HashMap<URL, Entry> targetFiles;

    // The database we use for storing our data.
    private SQLiteDatabase database = null;
	
	// Time in ms UTC at which we last checked for data.  0 means
	// never.
	private long lastDataCheck = 0;
	
}

