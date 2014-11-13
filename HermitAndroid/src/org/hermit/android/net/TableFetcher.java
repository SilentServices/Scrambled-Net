
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
import java.net.URL;
import java.net.URLConnection;
import java.util.Calendar;
import java.util.TimeZone;

import android.content.ContentValues;


/**
 * Concrete instance of WebFetcher which fetches tabular data from the web.
 * Lines in the file are parsed into fields and passed to the caller
 * in a ContentValues object.
 */
public class TableFetcher
    extends WebFetcher
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Fetch tabular data from a file on the web.
	 * 
	 * @param	url				The URL to fetch data from.
	 * @param	client			Client to pass the data to.
	 * @param	timeout			Maximum time in ms for which the job will be
	 * 							allowed to run.
	 * @param	ldate			If true, parse dates in long format,
	 * 							else false.
	 * @param	fields			Names to give to the fields in the results.
	 * @param	newer			If-modified-since time in ms UTC.  The
	 * 							fetch will only be carried out if the remote
	 * 							resource has been modified since this time.
	 * 							Also, any records with timestamps older than
	 * 							this are ignored.
	 * 							If zero, fetch without this condition.
	 */
	TableFetcher(URL url, Listener client, long timeout,
				 boolean ldate, String[] fields, long newer)
	{
		super(url, client, timeout, newer);

		init(client, timeout, ldate, fields, newer);
	}
	

    /**
     * Fetch tabular data from several files on the web.
     * 
     * @param   urls            The URLs to fetch data from.
     * @param   client          Client to pass the data to.
     * @param   timeout         Maximum time in ms for which the job will be
     *                          allowed to run.
     * @param   ldate           If true, parse dates in long format,
     *                          else false.
     * @param   fields          Names to give to the fields in the results.
     * @param   newer           If-modified-since time in ms UTC.  The
     *                          fetch will only be carried out if the remote
     *                          resource has been modified since this time.
     *                          Also, any records with timestamps older than
     *                          this are ignored.
     *                          If zero, fetch without this condition.
     */
    TableFetcher(URL[] urls, Listener client, long timeout,
                 boolean ldate, String[] fields, long newer)
    {
        super(urls, client, timeout, newer);

        init(client, timeout, ldate, fields, newer);
    }
    
	
    /**
     * Set up this instance.
     * 
     * @param   client          Client to pass the data to.
     * @param   timeout         Maximum time in ms for which the job will be
     *                          allowed to run.
     * @param   ldate           If true, parse dates in long format,
     *                          else false.
     * @param   fields          Names to give to the fields in the results.
     * @param   newer           If-modified-since time in ms UTC.  The
     *                          fetch will only be carried out if the remote
     *                          resource has been modified since this time.
     *                          Also, any records with timestamps older than
     *                          this are ignored.
     *                          If zero, fetch without this condition.
     */
	private final void init(Listener client, long timeout,
                            boolean ldate, String[] fields, long newer)
	{
        longDates = ldate;
        fieldNames = fields;

        TimeZone utc = TimeZone.getTimeZone("UTC");
        dateCal = Calendar.getInstance(utc);
        dateCal.clear();
        
        dateFields = new int[6];
	}

	
	// ******************************************************************** //
	// Data Fetching.
	// ******************************************************************** //

	/**
	 * Fetch a page of data from the given BufferedReader.
	 * 
	 * @param	url				The URL we're reading.
	 * @param	conn			The current connection to the URL.
	 * @param	readc			The BufferedReader to read from.
     * @throws  FetchException  Some problem was detected.
     * @throws  IOException     An I/O error occurred.
	 */
	@Override
	protected void handle(URL url, URLConnection conn, BufferedReader readc)
		throws FetchException, IOException
	{
		// Get the date of the file.
		long date = conn.getLastModified();
		
		ContentValues rec = new ContentValues();
		int stat;
		int count = 0;
		while ((stat = parse(readc, rec)) >= 0) {
	   		// If we were killed, bomb out.
    		if (isInterrupted())
    			throw new FetchException("timed out");

			if (stat >= 1)
				dataClient.onWebData(url, rec, date);
			
			if (++count % 20 == 0) {
                // Just don't hog the CPU.
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new FetchException("interrupted");
                }
			}
		}
	}

	
	/**
	 * Parse a line from a data file.
	 * 
	 * The files we're interested in have lines with space-separated fields.
	 *   * If longDates == false, the first three are year, month, day,
	 *     and the rest are float values.
	 *   * If longDates == true, the first five are year, month, day,
	 *     julian, dayseconds, and the rest are float values.
	 * Lines starting with ':' or '#' are comments.
	 * 
	 * @param	readc		The BufferedReader to read from.
	 * @param	values		The buffer to put the data in.  This is the
	 * 						parsed record, with a "date" field added,
	 * 						containing the timestamp of the record in ms
	 * 						UTC.
	 * @return				-1 on EOF; 0 on no data (e.g. end of line); 1
	 * 						if got data, which is now in values.
	 */
	private int parse(BufferedReader readc, ContentValues values) throws IOException {
    	// Get the date and time (UTC).  Note that for long dates we
		// need to skip 2 extra fields.
		int nDate = longDates ? 6 : 3;
		int nread = parseInts(readc, nDate, dateFields);
		if (nread < 0)
			return -1;
		else if (nread < nDate)
			return 0;
	
		dateCal.set(Calendar.YEAR, dateFields[0]);
		dateCal.set(Calendar.MONTH, dateFields[1] - 1);
		dateCal.set(Calendar.DAY_OF_MONTH, dateFields[2]);
		if (longDates) {
			final int time = dateFields[3];
			dateCal.set(Calendar.HOUR_OF_DAY, time / 100);
			dateCal.set(Calendar.MINUTE, time % 100);
		} else {
			dateCal.set(Calendar.HOUR_OF_DAY, 0);
			dateCal.set(Calendar.MINUTE, 0);
		}
		long time = dateCal.getTimeInMillis();

		// If this record is too old, forget it.
		if (time < newerThanDate)
			return 0;

		// Get the remaining parameters.  Add the timestamp.
		values.clear();
		nread = parseFloats(readc, values, fieldNames);
		if (nread < 0)
			return -1;
		
		values.put("date", time);
		return 1;
	}


	/**
	 * Parse some integer values from a data file.
	 * 
	 * @param	readc			The BufferedReader to read from.
	 * @param	count			The number of ints to read.
	 * @param	results			Array to place the results in.
	 * @return					The number of ints read.  0 on a blank
	 * 							line; -1 on EOF.
	 * @throws IOException 
	 */
	private int parseInts(BufferedReader readc, int count, int[] results)
		throws IOException
	{
		int c;

		boolean first = true;
		boolean inNum = false;
		int index = 0;
		int val = 0;
		int sign = 1;
		while ((c = readc.read()) >= 0) {
			// Look for a comment.  Skip to the newline.
			if (first && (c == ':' || c == '#')) {
				while ((c = readc.read()) >= 0)
					if (c == '\n' || c == '\r')
						break;
				return 0;
			}
			first = false;

			if (c == '\n' || c == '\r') {
				if (inNum)
					results[index++] = val * sign;
				return index;
			} else if (c == '-') {
				if (inNum) {
					results[index++] = val * sign;
					if (index >= count)
						return index;
				}
				inNum = true;
				val = 0;
				sign = -1;
			} else if (c >= '0' && c <= '9') {
				if (!inNum) {
					inNum = true;
					val = 0;
					sign = 1;
				}
				val = val * 10 + (c - '0');
			} else {
				if (inNum) {
					inNum = false;
					results[index++] = val * sign;
					if (index >= count)
						return index;
				}
			}
		}
		
        if (inNum) {
            inNum = false;
            results[index++] = val * sign;
        }
        return index;
	}


	/**
	 * Parse some float values from a data file.
	 * 
	 * @param	readc			The BufferedReader to read from.
	 * @param	values			The values map to place the results in.
	 * @param	results			Names of the fields to save in values.
	 * @return					-1 on EOF; 0 on EOL; 1 if got a float and EOL;
	 * 							2 if just got a float.
	 * @throws IOException 
	 */
	private int parseFloats(BufferedReader readc, ContentValues values, String[] fields)
		throws IOException
	{
		int index = 0;
		
		State state = State.SPACE;
		int intv = 0;
		int sign = 1;
		int fracv = 0;
		int fracDiv = 1;
		int expv = 0;
		int esign = 1;
		
		int c;
		while ((c = readc.read()) >= 0) {
			if (c == '\n' || c == '\r') {
				if (state != State.SPACE) {
					if (index < fieldNames.length) {
						float val = (intv +
									   ((float) fracv / (float) fracDiv)) *
									   (float) Math.pow(10, expv * esign) *
									   sign;
						values.put(fields[index++], val);
					}
				}
				return index;
			} else if (c == '+' || c == '-') {
				if (state == State.SPACE) {
					state = State.MAN;
					intv = 0;
					sign = c == '-' ? -1 : 1;
					fracv = 0;
					fracDiv = 1;
					expv = 0;
					esign = 1;
				} else if (state == State.MAN)
					sign = c == '-' ? -1 : 1;
				else if (state == State.EXP)
					esign = c == '-' ? -1 : 1;
			} else if (c >= '0' && c <= '9') {
				if (state == State.SPACE) {
					state = State.MAN;
					intv = 0;
					sign = 1;
					fracv = 0;
					fracDiv = 1;
					expv = 0;
					esign = 1;
				}
				if (state == State.MAN)
					intv = intv * 10 + (c - '0');
				else if (state == State.FRAC) {
					fracv = fracv * 10 + (c - '0');
					fracDiv *= 10;
				} else if (state == State.EXP)
					expv = expv * 10 + (c - '0');
			} else if (c == '.') {
				if (state == State.SPACE) {
					state = State.FRAC;
					intv = 0;
					sign = 1;
					fracv = 0;
					fracDiv = 1;
					expv = 0;
					esign = 1;
				} else if (state == State.MAN)
					state = State.FRAC;
			} else if (c == 'e' || c == 'E') {
				if (state == State.MAN || state == State.FRAC)
					state = State.EXP;
			} else if (c == ' ' || c == '\t' || true) {
				if (state != State.SPACE) {
					if (index < fieldNames.length) {
						float val = (intv + ((float) fracv / (float) fracDiv)) *
									   (float) Math.pow(10, expv * esign) *
									   sign;
						values.put(fields[index++], val);
					}
					state = State.SPACE;
				}
			}
		}

		return -1;
	}


    // ******************************************************************** //
    // Private Types.
    // ******************************************************************** //
	
	// Float parsing state.
	private static enum State { SPACE, MAN, FRAC, EXP };


    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //
    
    // Debugging tag.
    @SuppressWarnings("unused")
    private static final String TAG = "CachedFile";

    
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// True if the source uses long dates.
	private boolean longDates;

	// Names of the fields to parse and return.
	private String[] fieldNames;

	// Calendar used for date processing.
	private Calendar dateCal;
	
	// Int array used for date processing.
	private int[] dateFields;
	
}

