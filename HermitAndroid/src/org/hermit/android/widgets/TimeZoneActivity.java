
/**
 * widgets: useful add-on widgets for Android.
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


package org.hermit.android.widgets;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import java.util.TimeZone;

import org.hermit.utils.TimeUtils;

import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;


/**
 * This class displays a picker which can be used to select a timezone.
 * Options are provided to add app-specific zones, such as "local",
 * "nautical", etc.
 */
public class TimeZoneActivity
	extends ListActivity
{

	// ******************************************************************** //
    // Activity Lifecycle.
    // ******************************************************************** //

    /**
	 * Called when the activity is starting.  This is where most
	 * initialization should go: calling setContentView(int) to inflate
	 * the activity's UI, etc.
	 * 
	 * @param	icicle			If the activity is being re-initialized
	 * 							after previously being shut down then this
	 * 							Bundle contains the data it most recently
	 * 							supplied in onSaveInstanceState(Bundle).
	 * 							Note: Otherwise it is null.
     */
    @Override
    protected void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		
        // Get our passed-in intent.
        Intent intent = getIntent();
        
        // Build the master list of timezones, if we haven't yet.
        if (zoneList == null)
        	listZones(intent);
        
        // Create the adapter for the zone list.  Map the required fields
        // into the list item view.
        String[] from = { "name", "offset" };
        int[] to = { android.R.id.text1, android.R.id.text2 };
        SimpleAdapter adapter =
        	  			new SimpleAdapter(this, zoneList,
        							  	  android.R.layout.two_line_list_item,
        							  	  from, to);
        
        // Bind to our new adapter.
        setListAdapter(adapter);
    }

    
	// ******************************************************************** //
	// Zone Data Management.
	// ******************************************************************** //

    /**
     * Build the global list of timezones.
     * 
     * @param	intent			Intent containing parameters for this picker.
     */
    private static void listZones(Intent intent) {
    	// Create the list.  This is a mapping of time zone offsets
    	// to timezones which have that raw offset.
		zoneMap = new HashMap<Integer, ArrayList<TimeZone>>();
		
		// Build the offset to zone mapping, by scanning all existing zones.
		// TODO: due to a stupid bug, TimeZone reports the wrong zone
		// name (not zone ID) for many zones.  E.g. there are at least
		// two different zones reported as "Pacific Standard Time".  So
		// this algorithm to winnow down the zone list by using the zone
		// name doesn't work.
		//		String[] znames = TimeZone.getAvailableIDs();
		//		for (int i = 0; i < znames.length; ++i) {
		//			// Get the zone and its offset.
		//			TimeZone zone = TimeZone.getTimeZone(znames[i]);
		//			String name = zone.getDisplayName();
		//			int offset = zone.getRawOffset();
			
		for (String[] zoneNames : HARD_ZONES) {
			// Get the zone and its offset.
			String id = zoneNames[0];
			String name = zoneNames[1];
			TimeZone zone = TimeZone.getTimeZone(id);
			int offset = zone.getRawOffset();
			
			// Skip all the "GMT+x" zones.
			if (name.startsWith("GMT"))
				continue;
			
			// If we don't have any zones for this offset, create the list
			// and add it to the hash map.  Else, add this zone to the list
			// for the offset, but only if there isn't an equivalent zone
			// already there.
			if (!zoneMap.containsKey(offset)) {
				ArrayList<TimeZone> list = new ArrayList<TimeZone>();
				list.add(zone);
				zoneMap.put(offset, list);
			} else {
				ArrayList<TimeZone> list = zoneMap.get(offset);
				// TODO: So this doesn't work...
				//				boolean got = false;
				//				for (TimeZone other : list) {
				//					String oname = other.getDisplayName();
				//					if (other.hasSameRules(zone) && oname.equals(name)) {
				//						got = true;
				//						break;
				//					}
				//				}
				//				if (!got)
				//					list.add(zone);
				list.add(zone);
			}
		}
		
		// Create a sorted list of all the offsets.
		Set<Integer> keySet = zoneMap.keySet();
		Integer[] keys = new Integer[keySet.size()];
		keySet.toArray(keys);
		java.util.Arrays.sort(keys);
	
		// Now build the key-value list of timezones.
		zoneList = new ArrayList<HashMap<String, String>>();
		
		// Add an extra zone if requested.
		if (intent != null) {
			String id = intent.getStringExtra("addZoneId");
			String off = intent.getStringExtra("addZoneOff");
			if (off == null)
				off = "";
			if (id != null) {
				HashMap<String, String> map = new HashMap<String, String>();
				map.put("id", id);
				map.put("name", id);
				map.put("offset", off);
				zoneList.add(map);
			}
		}
		
		// Add system zones.
		for (int offset : keys) {
			ArrayList<TimeZone> list = zoneMap.get(offset);
			for (TimeZone zone : list) {
				HashMap<String, String> map = new HashMap<String, String>();
				// TODO: we really want the display name, when it works.
				// map.put("name", zone.getDisplayName());
				map.put("id", zone.getID());
				map.put("name", zone.getID());
				map.put("offset", TimeUtils.formatOffset(zone));
				zoneList.add(map);
			}
		}
    }
    
    
	// ******************************************************************** //
	// Listener.
	// ******************************************************************** //
    
    /**
     * This method will be called when an item in the list is selected.
     * Subclasses can call getListView().getItemAtPosition(position) if
     * they need to access the data associated with the selected item.
     * 
     * @param	l			The ListView where the click happened.
     * @param	v			The view that was clicked within the ListView.
     * @param	position	The position of the view in the list.
     * @param	id			The row id of the item that was clicked.
     */
    @Override
	public void onListItemClick(ListView l, View v, int position, long id) {
    	// Get the zone that was clicked.
    	HashMap<String, String> item = zoneList.get(position);
    	
    	// Create an intent containing the return data.
    	Intent data = new Intent();
    	data.putExtra("zoneId", item.get("id"));
    	
    	// And return to the calling activity.
		setResult(RESULT_OK, data);
		finish();
    }

    
	// ******************************************************************** //
	// Class Data.
	// ******************************************************************** //

    // Crappy hardwired list of timezones and their user-visible names.
    // See the note in listZones().
    private static final String[][] HARD_ZONES = {
        { "Pacific/Majuro", "Marshall Islands" },
        { "Pacific/Midway", "Midway Island" },
        { "Pacific/Honolulu", "Hawaii" },
        { "America/Anchorage", "Alaska" },
        { "America/Los_Angeles", "Pacific Time" },
        { "America/Tijuana", "Tijuana" },
        { "America/Phoenix", "Arizona" },
        { "America/Chihuahua", "Chihuahua" },
        { "America/Denver", "Mountain Time" },
        { "America/Costa_Rica", "Central America" },
        { "America/Chicago", "Central Time" },
        { "America/Mexico_City", "Mexico City" },
        { "America/Regina", "Saskatchewan" },
        { "America/Bogota", "Bogota" },
        { "America/New_York", "Eastern Time" },
        { "America/Caracas", "Venezuela" },
        { "America/Barbados", "Atlantic Time" },
        { "America/Manaus", "Manaus" },
        { "America/Santiago", "Santiago" },
        { "America/St_Johns", "Newfoundland" },
        { "America/Araguaina", "Brasilia" },
        { "America/Argentina/Buenos_Aires", "Buenos Aires" },
        { "America/Godthab", "Greenland" },
        { "America/Montevideo", "Montevideo" },
        { "Atlantic/South_Georgia", "Mid-Atlantic" },
        { "Atlantic/Azores", "Azores" },
        { "Atlantic/Cape_Verde", "Cape Verde Islands" },
        { "Africa/Casablanca", "Casablanca" },
        { "Europe/London", "London, Dublin" },
        { "Europe/Amsterdam", "Amsterdam, Berlin" },
        { "Europe/Belgrade", "Belgrade" },
        { "Europe/Brussels", "Brussels" },
        { "Europe/Sarajevo", "Sarajevo" },
        { "Africa/Windhoek", "Windhoek" },
        { "Africa/Brazzaville", "W. Africa Time" },
        { "Asia/Amman", "Amman, Jordan" },
        { "Europe/Athens", "Athens, Istanbul" },
        { "Asia/Beirut", "Beirut, Lebanon" },
        { "Africa/Cairo", "Cairo" },
        { "Europe/Helsinki", "Helsinki" },
        { "Asia/Jerusalem", "Jerusalem" },
        { "Europe/Minsk", "Minsk" },
        { "Africa/Harare", "Harare" },
        { "Asia/Baghdad", "Baghdad" },
        { "Europe/Moscow", "Moscow" },
        { "Asia/Kuwait", "Kuwait" },
        { "Africa/Nairobi", "Nairobi" },
        { "Asia/Tehran", "Tehran" },
        { "Asia/Baku", "Baku" },
        { "Asia/Tbilisi", "Tbilisi" },
        { "Asia/Yerevan", "Yerevan" },
        { "Asia/Dubai", "Dubai" },
        { "Asia/Kabul", "Kabul" },
        { "Asia/Karachi", "Islamabad, Karachi" },
        { "Asia/Oral", "Ural'sk" },
        { "Asia/Yekaterinburg", "Yekaterinburg" },
        { "Asia/Calcutta", "Kolkata" },
        { "Asia/Colombo", "Sri Lanka" },
        { "Asia/Katmandu", "Kathmandu" },
        { "Asia/Almaty", "Astana" },
        { "Asia/Rangoon", "Yangon" },
        { "Asia/Krasnoyarsk", "Krasnoyarsk" },
        { "Asia/Bangkok", "Bangkok" },
        { "Asia/Hong_Kong", "Beijing, Hong Kong" },
        { "Asia/Irkutsk", "Irkutsk" },
        { "Asia/Kuala_Lumpur", "Kuala Lumpur" },
        { "Australia/Perth", "Perth" },
        { "Asia/Taipei", "Taipei" },
        { "Asia/Seoul", "Seoul" },
        { "Asia/Tokyo", "Tokyo, Osaka" },
        { "Asia/Yakutsk", "Yakutsk" },
        { "Australia/Adelaide", "Adelaide" },
        { "Australia/Darwin", "Darwin" },
        { "Australia/Brisbane", "Brisbane" },
        { "Australia/Hobart", "Hobart" },
        { "Australia/Sydney", "Sydney, Canberra" },
        { "Asia/Vladivostok", "Vladivostok" },
        { "Pacific/Guam", "Guam" },
        { "Asia/Magadan", "Magadan" },
        { "Pacific/Auckland", "Auckland" },
        { "Pacific/Fiji", "Fiji" },
        { "Pacific/Tongatapu", "Tonga" },
    };
    
	// Mapping of all the timezones we're interested in.  This is a mapping
	// of time zone offsets to timezones which have that raw offset.
	private static HashMap<Integer, ArrayList<TimeZone>> zoneMap = null;
	
	// Listing of all the timezones we're interested in.  This is a list
	// of maps, each of which represents one zone.  Each mapping maps
	// field names to values.
	private static ArrayList<HashMap<String, String>> zoneList = null;

}

