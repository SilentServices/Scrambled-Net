
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


import org.hermit.android.R;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TimePicker;


/**
 * A dialog that prompts the user for the time of day using a {@link TimePicker}.
 *
 * <p>See the <a href="{@docRoot}resources/tutorials/views/hello-timepicker.html">Time Picker
 * tutorial</a>.</p>
 */
public class TimeoutPickerDialog extends AlertDialog implements OnClickListener, 
        TimeoutPicker.OnTimeChangedListener {

    /**
     * The callback interface used to indicate the user is done filling in
     * the time (they clicked on the 'Set' button).
     */
    public interface OnTimeSetListener {

        /**
         * @param view The view associated with this listener.
         * @param hourOfDay The hour that was set.
         * @param minute The minute that was set.
         */
        void onTimeSet(TimeoutPicker view, long millis);
    }

    private static final String MILLIS = "millis";
    
    private final TimeoutPicker mTimePicker;
    private final OnTimeSetListener mCallback;
    
    private long mInitialMillis;

    /**
     * @param context Parent.
     * @param callBack How parent is notified.
     * @param timeoutMillis The initial tiume in ms.
     */
    public TimeoutPickerDialog(Context context,
            OnTimeSetListener callBack,
            long timeoutMillis) {
        super(context);
        mCallback = callBack;
        mInitialMillis = timeoutMillis;

        updateTitle(mInitialMillis);
        
        setButton(context.getText(R.string.timeout_set), this);
        setButton2(context.getText(R.string.timeout_cancel), (OnClickListener) null);
        setIcon(android.R.drawable.ic_dialog_info);
        
        LayoutInflater inflater = 
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.timeout_picker_dialog, null);
        setView(view);
        mTimePicker = (TimeoutPicker) view.findViewById(R.id.timeoutPicker);

        // initialize state
        mTimePicker.setMillis(mInitialMillis);
        mTimePicker.setOnTimeChangedListener(this);
    }
    
    @Override
	public void onClick(DialogInterface dialog, int which) {
        if (mCallback != null) {
            mTimePicker.clearFocus();
            mCallback.onTimeSet(mTimePicker, mTimePicker.getMillis());
        }
    }

	@Override
	public void onTimeChanged(TimeoutPicker view, long millis) {
        updateTitle(millis);
	}

    public void updateTime(long millis) {
        mTimePicker.setMillis(millis);
    }

    private void updateTitle(long millis) {
        int secs = (int) millis / 1000;
        int mins = secs / 60 % 60;
        int hours = secs / 3600 % 100;
        secs %= 60;
        setTitle(String.format("%02d:%02d:%02d", hours, mins, secs));
    }
    
    @Override
    public Bundle onSaveInstanceState() {
        Bundle state = super.onSaveInstanceState();
        state.putLong(MILLIS, mTimePicker.getMillis());
        return state;
    }
    
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        long millis = savedInstanceState.getLong(MILLIS);
        mTimePicker.setMillis(millis);
        mTimePicker.setOnTimeChangedListener(this);
        updateTitle(millis);
    }
	
}

