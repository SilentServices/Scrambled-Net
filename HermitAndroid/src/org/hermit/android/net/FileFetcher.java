
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

import android.content.Context;


/**
 * Concrete instance of WebFetcher which gets a file and stores it locally.
 * The value passed back to the listener is the local file name as a File.
 */
public class FileFetcher
	extends WebFetcher
{

	// ******************************************************************** //
	// Constructor.
	// ******************************************************************** //

	/**
	 * Fetch a file from the web.
	 * 
	 * @param	context			Application context.
	 * @param	url				The URL to fetch data from.
	 * @param	name			Local name to save the file as.
	 * @param	client			Client to pass the data to.  It will be given
	 * 							the local file name as a File object.
	 * @param	timeout			Maximum time in ms for which the job will be
	 * 							allowed to run.
	 */
	public FileFetcher(Context context, URL url, String name,
					   Listener client, long timeout)
	{
		this(context, url, name, client, timeout, 0);
	}


	/**
	 * Fetch a file from the web.
	 * 
	 * @param	context			Application context.
	 * @param	url				The URL to fetch data from.
	 * @param	name			Local name to save the file as.
	 * @param	client			Client to pass the data to.  It will be given
	 * 							the local file name as a File object.
	 * @param	timeout			Maximum time in ms for which the job will be
	 * 							allowed to run.
	 * @param	newer			If-modified-since time in ms UTC.  The
	 * 							fetch will only be carried out if the remote
	 * 							resource has been modified since this time.
	 * 							If zero, fetch without this condition.
	 */
	public FileFetcher(Context context, URL url, String name,
					   Listener client, long timeout, long newer)
	{
		super(url, client, timeout, newer);
		this.context = context;
		this.fileName = name;
		
		filePath = context.getFileStreamPath(fileName);
		tempName = fileName + ".part";
		tempPath = new File(filePath.getParent(), tempName);
	}


	// ******************************************************************** //
	// Data Fetching.
	// ******************************************************************** //

	/**
	 * Fetch a page of data from the given stream.
	 * 
	 * @param	url				The URL we're reading.
	 * @param	conn			The current connection to the URL.
	 * @param	stream			The InputStream to read from.
     * @throws  FetchException  Some problem was detected.
	 * @throws  IOException     An I/O error occurred.
	 */
	@Override
	protected void handle(URL url, URLConnection conn, InputStream stream)
		throws FetchException, IOException
	{
		// Get the date of the file.
		long date = conn.getLastModified();

		try {
			// Read the file down and copy it to local storage.  If it fails,
			// it throws.
			fetchFile(url, conn, stream);
			
	   		// If we were killed, bomb out.
    		if (isInterrupted())
    			throw new FetchException("timed out");

			// OK, it worked.  Move the temp. file over the real file.
			filePath.delete();
			tempPath.renameTo(filePath);
			
	   		// If we were killed, bomb out.
    		if (isInterrupted())
    			throw new FetchException("timed out");

			// Notify the client that we're done.
			dataClient.onWebData(url, filePath, date);
		}
		finally {
			// Delete the temp. if it was left around.
			if (tempPath.exists())
				tempPath.delete();
		}
	}


	/**
	 * Fetch a page of data from the given stream.
	 * 
	 * @param	url				The URL we're reading.
	 * @param	conn			The current connection to the URL.
	 * @param	stream			The InputStream to read from.
	 * @throws IOException 
	 */
	private void fetchFile(URL url, URLConnection conn, InputStream stream)
		throws FetchException, IOException
	{
		// Read the file down and copy it to local storage.
		FileOutputStream fos = null;
		try {
			fos = context.openFileOutput(tempName, Context.MODE_PRIVATE);
			byte[] buf = new byte[8192];
			int count;
			while ((count = stream.read(buf)) >= 0) {
		   		// If we were killed, bomb out.
	    		if (isInterrupted())
	    			throw new FetchException("timed out");
	    		
				fos.write(buf, 0, count);
				
                // Just don't hog the CPU.
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new FetchException("interrupted");
                }
			}
		} finally {
			// On error, kill the file and bail.
			try {
				if (fos != null)
					fos.close();
			} catch (IOException e1) { }
		}
	}


	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //
	
    // Debugging tag.
	@SuppressWarnings("unused")
	private static final String TAG = "FileFetcher";

	
	// ******************************************************************** //
	// Private Data.
	// ******************************************************************** //
	
	// Application context.
	private Context context;
	
	// Local name for the fetched file.
	private String fileName;
	
	// Full path of the downloaded file.
	private File filePath;

	// Name and full path of the temp copy of the file as its downloading.
	private String tempName;
	private File tempPath;

}

