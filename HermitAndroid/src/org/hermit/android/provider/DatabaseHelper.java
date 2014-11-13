
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


import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;


/**
 * This class helps open, create, and upgrade the database file.
 * 
 * <p>Applications may use this class as is, or override it, for example to
 * provide a database upgrade handler.  If you don't wish to override it,
 * nothing need be done.  If you wish to subclass it, then create your
 * subclass and override {@link TableProvider#getHelper()} to return it.
 */
public class DatabaseHelper
    extends SQLiteOpenHelper
{

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //
    
    /**
     * Creater a helper instance.
     * 
     * @param   context         Application context.
     * @param   schema          Schema for this database.
     */
    public DatabaseHelper(Context context, DbSchema schema) {
        super(context, schema.getDbName(), null, schema.getDbVersion());
        dbSchema = schema;
    }

    
    // ******************************************************************** //
    // Database Create.
    // ******************************************************************** //

    /**
     * Called when the database is created for the first time.  This is
     * where the creation of tables and the initial population of the
     * tables should happen.
     * 
     * <p>The default implementation creates all the fields specified in
     * all of the table schemas.  Subclasses may override this, for example
     * to add special fields.
     * 
     * @param   db              The new database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        StringBuilder qb = new StringBuilder();
        for (TableSchema t : dbSchema.getDbTables()) {
            qb.setLength(0);
            qb.append("CREATE TABLE " + t.getTableName() + " ( ");
            TableSchema.FieldDesc[] fields = t.getTableFields();
            for (int i = 0; i < fields.length; ++i) {
            	TableSchema.FieldDesc field = fields[i];
            	if (i > 0)
            		qb.append(", ");
                qb.append(field.name);
                qb.append(" ");
                qb.append(field.type);
            }
            qb.append(" );");
            db.execSQL(qb.toString());
        }
    }

    
    // ******************************************************************** //
    // Database Upgrade.
    // ******************************************************************** //

    /**
     * Called when the database needs to be upgraded.  The implementation
     * should use this method to drop tables, add tables, or do anything
     * else it needs to upgrade to the new schema version.
     * 
     * <p>The default implementation simply deletes all tables and calls
     * {@link #onOpen(SQLiteDatabase)}.  Subclasses may override this method
     * to do a more intelligent upgrade.
     * 
     * <p>If you add new columns you can use ALTER TABLE to insert them into
     * a live table.  If you rename or remove columns you can use ALTER TABLE
     * to rename the old table, then create the new table and then populate
     * the new table with the contents of the old table.
     * 
     * @param   db              The new database.
     * @param   oldVersion      The old database version.
     * @param   newVersion      The new database version.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.w(TableProvider.TAG, "Upgrading database from version " +
        						 oldVersion + " to " + newVersion +
        						 ", which will destroy all old data");
        for (TableSchema t : dbSchema.getDbTables())
            db.execSQL("DROP TABLE IF EXISTS " + t.getTableName());
        onCreate(db);
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


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Our database schema.
    private final DbSchema dbSchema;
    
}

