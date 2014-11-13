
/**
 * org.hermit.android.provider: classes for building content providers.
 * 
 * These classes are designed to help build content providers in Android.
 *
 * <br>Copyright 2010-2011 Ian Cameron Smith
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


package org.hermit.android.provider;


import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;


/**
 * Utility class for managing a row from a content provider.
 */
public abstract class DbRow {

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a database row instance from a Cursor.
     * 
     * @param   schema      Schema of the table the row belongs to.
     * @param   c           Cursor to read the row data from.
     */
    protected DbRow(TableSchema schema, Cursor c) {
        this(schema, c, schema.getDefaultProjection());
    }

    
    /**
     * Create a database row instance from a Cursor.
     * 
     * @param   schema      Schema of the table the row belongs to.
     * @param   c           Cursor to read the row data from.
     * @param   projection  The fields to read.
     */
    protected DbRow(TableSchema schema, Cursor c, String[] projection) {
        tableSchema = schema;
        
        rowValues = new ContentValues();
        DatabaseUtils.cursorRowToContentValues(c, rowValues);
    }
    

    // ******************************************************************** //
    // Public Accessors.
    // ******************************************************************** //
    

    // ******************************************************************** //
    // Local Accessors.
    // ******************************************************************** //
    
    /**
     * Save the contents of this row to the given ContentValues.
     * 
     * @param   values          Object to write to.
     */
    void getValues(ContentValues values) {
        values.putAll(rowValues);
    }
    

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Schema of the table this row belongs to.
    @SuppressWarnings("unused")
	private final TableSchema tableSchema;
    
    // The values of the fields in this row.
    private final ContentValues rowValues;
    
}

