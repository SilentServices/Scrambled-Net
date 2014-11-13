
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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.Observable;
import java.util.TimeZone;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;


/**
 * This class implements a web-based source of data, which is cached
 * in a local database.  The data is assumed to be timestamped records.
 */
public class WebBasedData
	extends Observable
	implements WebFetcher.Listener
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //
	
	/**
	 * Create a web-based data source.
	 * 
	 * @param	name			The name of this source, and its database table.
	 * @param	base			The base URL for this source; this is the
	 * 							whole URL if urlSuff is null.
	 * @param	suff	 		The URL suffix.  If not null, the URL is
	 *							urlBase + date + urlSuff, where date is in
	 *							the format "20091231".
	 * @param	interval		Interval in ms between records in the source.
	 * @param	ldate			If true, the source uses long-format dates.
	 * @param	fields			Names for the fields in the source.
	 */
	public WebBasedData(String name, String base, String suff,
				 	    long interval, boolean ldate, String[] fields)
	{
		sourceName = name;
		urlBase = base;
		urlSuff = suff;
		dataInterval = interval;
		longDate = ldate;
		fieldNames = fields;
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
        Log.i(TAG, "WBD: create table " + sourceName);

		String create = "CREATE TABLE " + sourceName + " (" +
		  				"date INTEGER PRIMARY KEY UNIQUE ON CONFLICT IGNORE";
		for (String f : fieldNames)
			create += "," + f + " REAL";
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
        Log.i(TAG, "WBD: upgrade table " + sourceName);
        
        // We can simply dump all the old data, since the DB is just
    	// a cache of web data.
    	db.execSQL("DROP TABLE IF EXISTS " + sourceName);
    	createTable(db);
    }


	/**
	 * Set the database we use for storing our data.
     * 
     * @param	db			The database.  Will be null if the database
     *                      is being closed.
	 */
    public void setDatabase(SQLiteDatabase db) {
        long date = 0;
        synchronized (this) {
            database = db;
            if (database != null)
                date = findLatestDate();
        }

		// If we have any records, inform the client that we have data.
		if (date != 0) {
			setChanged();
			notifyObservers(latestDate);
		}
    }


	// ******************************************************************** //
	// Data Access.
	// ******************************************************************** //

    /**
     * Get the name of this source.
     * 
     * @return				The name of this source.
     */
    public synchronized String getName() {
		return sourceName;
    }
    

    /**
     * Query for all records we have stored.
     * 
     * It is the caller's responsibility to call close() on the cursor
     * when done.
     * 
     * @return				A Cursor over all the records, sorted in
     * 						ascending date order.  The Cursor is positioned
     * 						before the first record.
     */
    public synchronized Cursor allRecords() {
		return database.query(sourceName, fieldNames, null, null,
				  			  null, null, "date ASC", null);
    }
    

    /**
     * Query for all records we have stored timstamped AFTER a given date.
     * 
     * It is the caller's responsibility to call close() on the cursor
     * when done.
     * 
	 * @param	date		Cut-off date -- only return records newer
	 * 						than this date (not equal).
     * @return				A Cursor over all the records, sorted in
     * 						ascending date order.  The Cursor is positioned
     * 						before the first record.
     */
    public synchronized Cursor allRecordsSince(long date) {
		return database.query(sourceName, fieldNames,
							  "date>" + date, null,
				  			  null, null, "date ASC", null);
    }
    

    /**
     * Query for the newest record we have stored.
     * 
     * @return				The contents of the last record we have.
     * 						Returns null if there are no records.
     */
    public synchronized ContentValues lastRecord() {
		Cursor c = database.query(sourceName, fieldNames, null, null,
				  			      null, null, "date DESC", "1");
		if (!c.moveToFirst()) {
			c.close();
			return null;
		}
		
		ContentValues data = new ContentValues();
		DatabaseUtils.cursorRowToContentValues(c, data);
		c.close();

		return data;
    }
    
    
	// ******************************************************************** //
	// Update.
	// ******************************************************************** //

    /**
     * Check to see whether we need to update our stored data.  If so,
     * kick off a web fetch.
     * 
     * The observers will be notified for any new data that we load.
     * 
     * @param	now				The current time in millis.
     */
    public void update(long now) {
        if (database == null)
            return;
        
        URL[] urls = null;
        String name = null;
        long earliest = 0;
        
        synchronized (this) {
            // If it's too soon since the last check, don't even try.
            if (now - lastDataCheck < MIN_CHECK_INTERVAL) {
    	        Log.i(TAG, "WBD " + sourceName + ": update reject: too soon since last");
                return;
            }
        
            // Find the date of the newest data we have.  If it is less than
            // REFRESH_INTERVAL old, then do nothing.
            long latest = findLatestDate();
            if (now - latest < REFRESH_INTERVAL) {
                Log.i(TAG, "WBD " + sourceName + ": update reject: have fresh data");
                return;
            }

            // See how far back we need to go.  The earliest data we want is
            // the most recent we have plus DATA_INTERVAL; maximum
            // MAX_DATA_AGE old.
            earliest = latest + dataInterval;
            if (earliest < now - MAX_SAMPLES * dataInterval)
                earliest = now - MAX_SAMPLES * dataInterval;

            // Add the URL(s) to fetch.  If the URL for this source is
            // date-dependent, figure out which URLs to get.  Otherwise just
            // fetch the URL.
            try {
                if (urlSuff != null) {
                    String last = ymNameForDate(earliest);
                    String curr = ymNameForDate(now);
                    if (!last.equals(curr)) {
                        urls = new URL[] { new URL(urlBase + last + urlSuff),
                                new URL(urlBase + curr + urlSuff) };
                        name = last + "," + curr;
                    } else {
                        urls = new URL[] { new URL(urlBase + curr + urlSuff) };
                        name = curr;
                    }
                } else {
                    urls = new URL[] { new URL(urlBase) };
                    name = "fixed";
                }
            } catch (MalformedURLException e) {
                // Should never really happen.
                String msg = e.getClass().getName() + ": " + e.getMessage();
                Log.e(TAG, msg);
            }
        }

        Log.i(TAG, "WBD " + sourceName + ": update: kick off " + name);
        TableFetcher wf = new TableFetcher(urls, this, FETCH_TIMEOUT,
                longDate, fieldNames, earliest);
        wf.start();

        lastDataCheck = now;
    }


	/**
	 * Find the date of the newest record in the database for this
	 * source.
	 * 
	 * @return				The timestamp of the newest record in
	 *						our table, in ms UTC.
	 */
	private long findLatestDate() {
		String[] fields = new String[] { "date" };
		Cursor c = database.query(sourceName, fields, null, null,
								  null, null, "date DESC", "1");
		if (!c.moveToFirst())
			latestDate = 0;
		else {
			int cind = c.getColumnIndexOrThrow(fields[0]);
			latestDate = c.getLong(cind);
		}
		c.close();
		
		return latestDate;
	}
	
	
	/**
	 * Get the year and month name for a given date.
	 * 
	 * @param	date		The date in ms UTC.
	 * @return				Date year-month name, as in "20091231".
	 */
	private static String ymNameForDate(long date) {
		if (calendar == null)
			calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.setTimeInMillis(date);
		
		// The URLs we're after contain the date, so we have to construct them.
		// Each file is a calendar month of data -- of course we may be at
		// the start of a month.  So get this month's and last month's,
		// to guarantee 30 days * 24 hours.
		int year = calendar.get(Calendar.YEAR);
		int month = calendar.get(Calendar.MONTH) + 1;
		return String.format("%04d%02d", year, month);
	}


    // ******************************************************************** //
    // Fetched Data Handling.
    // ******************************************************************** //

	/**
	 * Optional processing step -- this is called for each record we read.
	 * It can be used to add synthesized values.
	 * 
	 * @param	rec				The record to process.
	 */
	protected void process(ContentValues rec) {
	}
	
	
	/**
	 * This method is invoked when a data record is retrieved from
	 * the URL we were invoked on.
	 * 
	 * @param	url				The URL of the source being loaded.
	 * @param	obj				The object that was loaded; the type
	 * 							depends on the fetcher class used.
	 * @param	fileDate		The last modified time of the source file,
	 * 							as reported by the server, in ms UTC.
	 */
	@Override
	public void onWebData(URL url, Object obj, long fileDate) {
	    if (database == null)
	        return;
	    
		if (!(obj instanceof ContentValues)) {
			onWebError("Loaded object for " + url + " not a ContentValues!");
			return;
		}
		ContentValues rec = (ContentValues) obj;
	    long rdate = rec.getAsLong("date");
		
		// Do any required processing.
		process(rec);
		
		// Add it to the database.
		synchronized (this) {
		    // Update the latest date if this is a newer record.
		    if (rdate > latestDate) {
		        database.insert(sourceName, fieldNames[0], rec);
		        latestDate = rdate;
		    }
		}
	}


	/**
	 * This method is invoked when the given URL has been fully fetched.
	 */
	@Override
	public void onWebDone() {
		synchronized(this) {
	        if (database != null) {
	            // Trim off any old unwanted values.
	            long earliest = System.currentTimeMillis() - MAX_SAMPLES * dataInterval;
	            database.delete(sourceName, "date<" + earliest, null);
	        }
		}

		// Inform the client that we have data.
		setChanged();
		notifyObservers(latestDate);
	}


	/**
	 * Handle an error while fetching web data.
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
	private static final String TAG = "WebBasedData";

	// Calendar used for date manipulations.
	private static Calendar calendar = null;

	// Minimum interval in ms between checks for file updates.
	private static final long MIN_CHECK_INTERVAL = 30 * 60 * 1000;
	
	// The interval in ms after which we will look for updated data.
	private static final long REFRESH_INTERVAL = 120 * 60 * 1000;
	
	// The maximum number of samples we will store.
	private static final long MAX_SAMPLES = 400;
	
	// The timeout in ms for fetching a file.
	private static final long FETCH_TIMEOUT = 15 * 60 * 1000;


	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //

	// The name of this source, and its database table.
	private final String sourceName;

	// The base URL for this source, and the suffix.  If urlSuff is null,
	// then urlBase is the whole URL; else, the URL is
	// urlBase + ymNameForDate(long date) + urlSuff.
	private final String urlBase;
	private final String urlSuff;

	// Interval in ms between records in the source.
	private long dataInterval;

	// If true, the source uses long date format.
	private final boolean longDate;

	// The names to assign to the fields from the source.
	private final String[] fieldNames;

    // The database we use for storing our data.
    private SQLiteDatabase database = null;

	// Date in ms UTC of the latest record in the DB for this source.
	// 0 means no data.
	private long latestDate = 0;

	// Date in ms UTC at which we last checked for data.  0 means
	// never.
	private long lastDataCheck = 0;

}

