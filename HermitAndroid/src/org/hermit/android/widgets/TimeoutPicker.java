
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


import java.util.Calendar;

import org.hermit.android.R;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import org.hermit.android.widgets.NumberPicker;


/**
 * A view for selecting an elapsed time, in hours, minutes, and seconds.
 * The hour, minutes, and seconds are controlled by vertical spinners.
 *
 * The hour can be entered by keyboard input.  Entering in two digit hours
 * can be accomplished by hitting two digits within a timeout of about a
 * second (e.g. '1' then '2' to select 12).
 *
 * The minutes can be entered by entering single digits.
 */
public class TimeoutPicker
	extends FrameLayout
{

	// ******************************************************************** //
    // Public Types.
    // ******************************************************************** //

    /**
     * The callback interface used to indicate the time has been adjusted.
     */
    public interface OnTimeChangedListener {

    	/**
    	 * @param view The view associated with this listener.
    	 * @param millis The currently set time interval in milliseconds.
    	 */
    	void onTimeChanged(TimeoutPicker view, long millis);
    }


	// ******************************************************************** //
    // Constructors.
    // ******************************************************************** //

    public TimeoutPicker(Context context) {
        this(context, null);
    }
    
    
    public TimeoutPicker(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }
    

    public TimeoutPicker(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);

        LayoutInflater inflater =
                (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        inflater.inflate(R.layout.timeout_picker, this, true);

        // Configure the hour picker.
        hourPicker = (NumberPicker) findViewById(R.id.hour);
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(99);
        hourPicker.setOnLongPressUpdateInterval(100);
        hourPicker.setFormatter(TWO_DIGIT_FORMATTER);
        hourPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
			@Override
			public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                currentHour = newVal;
                onTimeChanged();
            }
        });

        // Configure the minutes picker.
        minutePicker = (NumberPicker) findViewById(R.id.minute);
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(59);
        minutePicker.setOnLongPressUpdateInterval(100);
        minutePicker.setFormatter(TWO_DIGIT_FORMATTER);
        minutePicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
			@Override
			public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                currentMinute = newVal;
                onTimeChanged();
			}
		});

        // Configure the seconds picker.
        secondPicker = (NumberPicker) findViewById(R.id.second);
        secondPicker.setMinValue(0);
        secondPicker.setMaxValue(59);
        secondPicker.setOnLongPressUpdateInterval(100);
        secondPicker.setFormatter(TWO_DIGIT_FORMATTER);
        secondPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
			@Override
			public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                currentSecond = newVal;
                onTimeChanged();
			}
		});

        // initialize to current time
        Calendar cal = Calendar.getInstance();
        
        // by default we're not in 24 hour mode
        setCurrentHour(cal.get(Calendar.HOUR_OF_DAY));
        setCurrentMinute(cal.get(Calendar.MINUTE));
        
        if (!isEnabled())
            setEnabled(false);
    }
    

	// ******************************************************************** //
    // Configuration.
    // ******************************************************************** //

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        secondPicker.setEnabled(enabled);
        minutePicker.setEnabled(enabled);
        hourPicker.setEnabled(enabled);
    }


    /**
     * Set the callback that indicates the time has been adjusted by the user.
     * @param onTimeChangedListener the callback, should not be null.
     */
    public void setOnTimeChangedListener(OnTimeChangedListener onTimeChangedListener) {
        changeListener = onTimeChangedListener;
    }

    
    /**
     * @return The current time value in ms.
     */
    public long getMillis() {
    	int secs = currentHour * 3600 + currentMinute * 60 + currentSecond;
        return (long) (secs * 1000);
    }

    
    /**
     * Set the current time value.
     * 
     * @param	millis			Time value to set, in ms.
     */
    public void setMillis(long millis) {
        currentSecond = (int) ((millis / 1000) % 60);
        currentMinute = (int) ((millis / 60000) % 60);
        currentHour = (int) ((millis / 3600000) % 100);
        secondPicker.setValue(currentSecond);
        minutePicker.setValue(currentMinute);
        hourPicker.setValue(currentHour);
        onTimeChanged();
    }


    /**
     * @return The current hour (0-99).
     */
    public int getCurrentHour() {
        return currentHour;
    }

    
    /**
     * Set the current hour (0-99).
     */
    public void setCurrentHour(int hour) {
        currentHour = hour;
        hourPicker.setValue(currentHour);
        onTimeChanged();
    }

    
    /**
     * @return The current minute.
     */
    public int getCurrentMinute() {
        return currentMinute;
    }

    
    /**
     * Set the current minute (0-59).
     */
    public void setCurrentMinute(int min) {
        currentMinute = min;
        minutePicker.setValue(currentMinute);
        onTimeChanged();
    }
    
    
    /**
     * @return The current second.
     */
    public int getCurrentSecond() {
        return currentSecond;
    }

    
    /**
     * Set the current second (0-59).
     */
    public void setCurrentSecond(int sec) {
        currentSecond = sec;
        secondPicker.setValue(currentSecond);
        onTimeChanged();
    }
    

    /**
     * Return the offset of the widget's text baseline from the
     * widget's top boundary.
     */
    @Override
    public int getBaseline() {
        return hourPicker.getBaseline(); 
    }
	  
    
	// ******************************************************************** //
	// Updating.
	// ******************************************************************** //

    private void onTimeChanged() {
    	if (changeListener != null)
    		changeListener.onTimeChanged(this, getMillis());
    }


	// ******************************************************************** //
	// Save and Restore.
	// ******************************************************************** //

    /**
     * Used to save / restore the state of the timeout picker.
     */
    private static class SavedState extends BaseSavedState {
        private SavedState(Parcelable superState, int hour, int minute, int sec) {
            super(superState);
            mHour = hour;
            mMinute = minute;
            mSecond = sec;
        }
        
        private SavedState(Parcel in) {
            super(in);
            mHour = in.readInt();
            mMinute = in.readInt();
            mSecond = in.readInt();
        }

        public int getHour() {
            return mHour;
        }

        public int getMinute() {
            return mMinute;
        }

        public int getSecond() {
            return mSecond;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mHour);
            dest.writeInt(mMinute);
            dest.writeInt(mSecond);
        }

//        public static final Parcelable.Creator<SavedState> CREATOR =
//                								new Creator<SavedState>() {
//            public SavedState createFromParcel(Parcel in) {
//                return new SavedState(in);
//            }
//
//            public SavedState[] newArray(int size) {
//                return new SavedState[size];
//            }
//        };

        private final int mHour;
        private final int mMinute;
        private final int mSecond;
    }
    

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        return new SavedState(superState, currentHour, currentMinute, currentSecond);
    }

    
    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState ss = (SavedState) state;
        super.onRestoreInstanceState(ss.getSuperState());
        setCurrentHour(ss.getHour());
        setCurrentMinute(ss.getMinute());
        setCurrentSecond(ss.getSecond());
    }

    
    // ******************************************************************** //
    // Private Types.
    // ******************************************************************** //

    /*
     * Use a custom NumberPicker formatting callback to use two-digit
     * minutes strings like "01".  Keeping a static formatter etc. is the
     * most efficient way to do this; it avoids creating temporary objects
     * on every call to format().
     */
    private static final NumberPicker.Formatter TWO_DIGIT_FORMATTER =
    										new NumberPicker.Formatter() {
		@Override
		public String format(int value) {
			charBuf[0] = (char) ('0' + value / 10 % 10);			
			charBuf[1] = (char) ('0' + value % 10);
    		return new String(charBuf);
		}
    	
    	private final char[] charBuf = new char[2];
    };


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // state
    private int currentHour = 0; // 0-99
    private int currentMinute = 0; // 0-59
    private int currentSecond = 0; // 0-59

    // ui components
    private final NumberPicker hourPicker;
    private final NumberPicker minutePicker;
    private final NumberPicker secondPicker;
   
    // Listener for time changes.  null if not set.
    private OnTimeChangedListener changeListener;
    
}

