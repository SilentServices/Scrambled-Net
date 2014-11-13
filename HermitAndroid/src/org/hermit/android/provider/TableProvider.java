
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


import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.provider.BaseColumns;
import android.text.TextUtils;
import android.util.Log;


/**
 * This class is a base for content providers which provide access to
 * table-organized data in an SQL database.
 * 
 * <p>Typically, this is used by creating a subclass which is empty
 * other than providing an appropriate schema to this class's constructor.
 * The bulk of the work in creating a content provider is in creating
 * the schema, a subclass of {@link DbSchema}.
 */
public class TableProvider
    extends ContentProvider
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create an instance of this content provider.
     * 
     * @param   schema      Structure defining the database schema.
     */
    public TableProvider(DbSchema schema) {
        dbSchema = schema;
        sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        String auth = schema.getDbAuth();
        int i = 0;
        for (TableSchema t : schema.getDbTables()) {
            String tn = t.getTableName();
            sUriMatcher.addURI(auth, tn, i);
            sUriMatcher.addURI(auth, tn + "/#", 0x10000 | i);
            Log.i(TAG, "Match " + auth + "/" + tn + "=" + i);
            ++i;
        }
    }
    
    
    // ******************************************************************** //
    // Initialization.
    // ******************************************************************** //

    /**
     * Called when the provider is being started.
     * 
     * @return          true if the provider was successfully loaded,
     *                  false otherwise.
     */
    @Override
    public boolean onCreate() {
        mOpenHelper = getHelper();
        return true;
    }


    // ******************************************************************** //
    // Accessors.
    // ******************************************************************** //

    /**
     * Get the database schema.
     * 
     * @return                  The schema for this database.
     */
    protected DbSchema getSchema() {
        return dbSchema;
    }


    /**
     * Get the schema for a specified table.
     * 
     * @param   name            The name of the table we want.
     * @return                  The schema for the given table.
     * @throws  IllegalArgumentException  No such table.
     */
    protected TableSchema getTableSchema(String name)
        throws IllegalArgumentException
    {
        return dbSchema.getTable(name);
    }


    // ******************************************************************** //
    // Database Helper.
    // ******************************************************************** //
 
    /**
     * Get the database helper which this content provider will use.
     * 
     * <p>Subclasses may override this to provide a smarter database
     * helper; for example, to implement a smarter database upgrade
     * process.  See {@link DatabaseHelper}.
     * 
     * @return              A database helper for this content provider.
     */
    protected DatabaseHelper getHelper() {
        return new DatabaseHelper(getContext(), getSchema());
    }
    

    // ******************************************************************** //
    // Data Access.
    // ******************************************************************** //
    
    /**
     * Return the MIME type of the data at the given URI.  This should
     * start with vnd.android.cursor.item/ for a single record, or
     * vnd.android.cursor.dir/ for multiple items.
     * 
     * @param   uri         The URI to query.
     * @return              MIME type string for the given URI, or null 
     *                      if there is no type. 
     */
    @Override
    public String getType(Uri uri) {
        int tindex = sUriMatcher.match(uri);
        if (tindex == UriMatcher.NO_MATCH)
            throw new IllegalArgumentException("Unknown URI " + uri +
                                               " in getType()");
        
        boolean isItem = (tindex & 0x10000) != 0;
        tindex &= 0xffff;
        if (tindex < 0 || tindex >= dbSchema.getDbTables().length)
            throw new IllegalArgumentException("Invalid table in " + uri +
                                               " in getType()");
        TableSchema t = dbSchema.getDbTables()[tindex];
        
        if (!isItem)
            return t.getTableType();
        else
            return t.getItemType();
    }
    

    /**
     * Receives a query request from a client in a local process, and
     * returns a Cursor.  This is called internally by the ContentResolver.
     * 
     * @param   uri         The URI to query.  This will be the full URI
     *                      sent by the client; if the client is requesting
     *                      a specific record, the URI will end in a record
     *                      number that the implementation should parse and
     *                      add to a WHERE or HAVING clause, specifying that
     *                      _id value.
     * @param   projection  The list of columns to put into the cursor.
     *                      If null all columns are included.
     * @param   where       A selection criteria to apply when filtering
     *                      rows.  If null then all rows are included.
     * @param   whereArgs   You may include ?s in selection, which will
     *                      be replaced by the values from selectionArgs,
     *                      in order that they appear in the selection.
     *                      The values will be bound as Strings.
     * @param   sortOrder   How the rows in the cursor should be sorted.
     *                      If null then the provider is free to define the
     *                      sort order.
     * @return              A Cursor or null. 
     */
    @Override
    public Cursor query(Uri uri, String[] projection,
                        String where, String[] whereArgs,
                        String sortOrder)
    {
        int tindex = sUriMatcher.match(uri);
        if (tindex == UriMatcher.NO_MATCH)
            throw new IllegalArgumentException("Unknown URI " + uri +
                                               " in query()");
        
        boolean isItem = (tindex & 0x10000) != 0;
        tindex &= 0xffff;
        if (tindex < 0 || tindex >= dbSchema.getDbTables().length)
            throw new IllegalArgumentException("Invalid table in " + uri +
                                               " in query()");
        TableSchema t = dbSchema.getDbTables()[tindex];

        Cursor c;
        if (isItem)
            c = queryItem(t, projection, uri.getPathSegments().get(1));
        else
            c = queryItems(t, projection, where, whereArgs, sortOrder);
        
        // Tell the cursor what uri to watch, so it knows when its
        // source data changes.
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    
    /**
     * Query for a specified item within a table.
     * 
     * @param   t           The schema for the table to query.
     * @param   projection  The list of columns to put into the cursor.
     *                      If null all columns are included.
     * @param   id          The ID of the item we want.
     * @return              A Cursor or null. 
     */
    protected Cursor queryItem(TableSchema t, String[] projection, long id) {
        return queryItem(t, projection, "" + id);
    }
    
    
    /**
     * Query for a specified item within a table.
     * 
     * @param   t           The schema for the table to query.
     * @param   projection  The list of columns to put into the cursor.
     *                      If null all columns are included.
     * @param   id          The ID of the item we want, as a String.
     * @return              A Cursor or null. 
     */
    protected Cursor queryItem(TableSchema t, String[] projection, String id) {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(t.getTableName());
        qb.setProjectionMap(t.getProjectionMap());
        qb.appendWhere(BaseColumns._ID + "=" + id);

        // Get the database and run the query.
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        return qb.query(db, projection, null, null, null, null, null);
    }
    

    /**
     * Query for items within a table.
     * 
     * @param   t           The schema for the table to query.
     * @param   projection  The list of columns to put into the cursor.
     *                      If null all columns are included.
     * @param   where       A selection criteria to apply when filtering
     *                      rows.  If null then all rows are included.
     * @param   whereArgs   You may include ?s in selection, which will
     *                      be replaced by the values from selectionArgs,
     *                      in order that they appear in the selection.
     *                      The values will be bound as Strings.
     * @param   sortOrder   How the rows in the cursor should be sorted.
     *                      If null then the provider is free to define the
     *                      sort order.
     * @return              A Cursor or null. 
     */
    protected Cursor queryItems(TableSchema t, String[] projection,
                                String where, String[] whereArgs,
                                String sortOrder)
    {
        SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
        qb.setTables(t.getTableName());
        qb.setProjectionMap(t.getProjectionMap());
        
        // If no sort order is specified, use the default for the table.
        if (TextUtils.isEmpty(sortOrder))
            sortOrder = t.getSortOrder();

        // Get the database and run the query.
        SQLiteDatabase db = mOpenHelper.getReadableDatabase();
        return qb.query(db, projection, where, whereArgs, null, null, sortOrder);
    }
    

    // ******************************************************************** //
    // Data Insertion.
    // ******************************************************************** //
    
    /**
     * This method is called prior to processing an insert; it is called
     * after {@link TableSchema#onInsert(ContentValues)}.  Subclasses
     * can use this to carry out additional processing.
     * 
     * @param   uri         The content:// URI of the insertion request.
     * @param   table       The schema of the table we're inserting into.
     * @param   initValues  A set of column_name/value pairs to add to
     *                      the database.
     */
    protected void onInsert(Uri uri, TableSchema table, ContentValues initValues) {
        
    }
    
    
    /**
     * Implement this to insert a new row.  As a courtesy, call
     * notifyChange() after inserting.
     * 
     * @param   uri         The content:// URI of the insertion request.
     * @param   initValues  A set of column_name/value pairs to add to
     *                      the database.
     * @return              The URI for the newly inserted item. 
     */
    @Override
    public Uri insert(Uri uri, ContentValues initValues) {
        int tindex = sUriMatcher.match(uri);
        if (tindex == UriMatcher.NO_MATCH)
            throw new IllegalArgumentException("Unknown URI " + uri +
                                               " in insert()");
        
        boolean isItem = (tindex & 0x10000) != 0;
        tindex &= 0xffff;
        if (tindex < 0 || tindex >= dbSchema.getDbTables().length)
            throw new IllegalArgumentException("Invalid table in " + uri +
                                               " in insert()");
        TableSchema t = dbSchema.getDbTables()[tindex];
        
        if (isItem)
            throw new IllegalArgumentException("Can't insert into item URI " +
                                               uri);

        // Copy the values so we can add to it.  Create it if needed.
        ContentValues values;
        if (initValues != null)
            values = new ContentValues(initValues);
        else
            values = new ContentValues();
        
        // Now, do type-specific setup, and fill in any missing values.
        t.onInsert(values);

        // Allow subclasses to do additional processing.
        onInsert(uri, t, values);

        // Insert the new row.
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        long rowId = db.insert(t.getTableName(), t.getNullHack(), values);
        if (rowId <= 0)
            throw new SQLException("Failed to insert row into " + uri);
        
        // Inform everyone about the change.
        Uri tableUri = t.getContentUri();
        Uri itemUri = ContentUris.withAppendedId(tableUri, rowId);
        getContext().getContentResolver().notifyChange(itemUri, null);
        
        return itemUri;
    }
    

    // ******************************************************************** //
    // Data Deletion.
    // ******************************************************************** //
    
    /**
     * A request to delete one or more rows.  The selection clause is
     * applied when performing the deletion, allowing the operation to
     * affect multiple rows in a directory.  As a courtesy, call
     * notifyDelete() after deleting.
     * 
     * The implementation is responsible for parsing out a row ID at the
     * end of the URI, if a specific row is being deleted.  That is, the
     * client would pass in content://contacts/people/22 and the
     * implementation is responsible for parsing the record number (22)
     * when creating an SQL statement.
     * 
     * @param   uri         The full URI to delete, including a row ID
     *                      (if a specific record is to  be deleted).
     * @param   where       An optional restriction to apply to rows when
     *                      deleting.
     * @param   whereArgs   You may include ?s in where, which will
     *                      be replaced by the values from whereArgs.
     * @return              The number of rows affected.
     * @throws  SQLException    Database error.
     */
    @Override
    public int delete(Uri uri, String where, String[] whereArgs) {
        int tindex = sUriMatcher.match(uri);
        if (tindex == UriMatcher.NO_MATCH)
            throw new IllegalArgumentException("Unknown URI " + uri +
                                               " in delete()");
        
        boolean isItem = (tindex & 0x10000) != 0;
        tindex &= 0xffff;
        if (tindex < 0 || tindex >= dbSchema.getDbTables().length)
            throw new IllegalArgumentException("Invalid table in " + uri +
                                               " in delete()");
        TableSchema t = dbSchema.getDbTables()[tindex];
        
        // If the URI specifies an item, add the item ID as a where condition.
        String whereClause;
        if (!isItem) {
            whereClause = where;
        } else {
            String rowId = uri.getPathSegments().get(1);
            whereClause = BaseColumns._ID + "=" + rowId;
            if (!TextUtils.isEmpty(where))
                whereClause += " AND (" + where + ')';
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.delete(t.getTableName(), whereClause, whereArgs);

        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }


    // ******************************************************************** //
    // Data Updating.
    // ******************************************************************** //
    
    /**
     * Update a content URI.  All rows matching the optionally provided
     * selection will have their columns listed as the keys in the values
     * map with the values of those keys.  As a courtesy, call notifyChange()
     * after updating.
     * 
     * @param   uri         The URI to update.  This can potentially have a
     *                      record ID if this is an update request for a
     *                      specific record.
     * @param   values      A Bundle mapping from column names to new column
     *                      values (NULL is a valid value).
     * @param   where       An optional restriction to apply to rows when
     *                      updating.
     * @param   whereArgs   You may include ?s in where, which will
     *                      be replaced by the values from whereArgs.
     * @return              The number of rows affected.
     */
    @Override
    public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
        int tindex = sUriMatcher.match(uri);
        if (tindex == UriMatcher.NO_MATCH)
            throw new IllegalArgumentException("Unknown URI " + uri +
                                               " in update()");
        
        boolean isItem = (tindex & 0x10000) != 0;
        tindex &= 0xffff;
        if (tindex < 0 || tindex >= dbSchema.getDbTables().length)
            throw new IllegalArgumentException("Invalid table in " + uri +
                                               " in update()");
        TableSchema t = dbSchema.getDbTables()[tindex];
        
        // If the URI specifies an item, add the item ID as a where condition.
        String whereClause;
        if (!isItem) {
            whereClause = where;
        } else {
            String rowId = uri.getPathSegments().get(1);
            whereClause = BaseColumns._ID + "=" + rowId;
            if (!TextUtils.isEmpty(where))
                whereClause += " AND (" + where + ')';
        }
        
        SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        int count = db.update(t.getTableName(), values, whereClause, whereArgs);
        
        getContext().getContentResolver().notifyChange(uri, null);
        return count;
    }

    
    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    static final String TAG = "TableProvider";
    

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Schema for this provider.
    private final DbSchema dbSchema;

    // The URI matcher determines, for a given URI, what is being accessed.
    private final UriMatcher sUriMatcher;

    // This content provider's database helper.
    private DatabaseHelper mOpenHelper;

}

