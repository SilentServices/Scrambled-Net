
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.hermit.android.provider.TableSchema.FieldDesc;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;


/**
 * Class encapsulating the schema for a content provider.  Applications
 * must subclass this, and provide the necessary information in the
 * call to this base class's constructor.
 * 
 * <p>An application's subclass will typically provide the following:
 * 
 * <ul>
 * <li>Inner classes which are subclasses of {@link TableSchema},
 *     defining the schemas of the individual tables.
 * <li>A constructor which calls this class's constructor, passing the
 *     required information.
 * </ul>
 */
public abstract class DbSchema {

    // ******************************************************************** //
    // Constructor.
    // ******************************************************************** //

    /**
     * Create a database schema instance.
     * 
     * @param   name        Name for the database; e.g. "passages".
     * @param   version     Version number of the database.  The upgrade
     *                      process will be run when this increments.
     * @param   auth        Authority name for this content provider; e.g.
     *                      "org.hermit.provider.PassageData".
     * @param   tables      List of table schemas.
     */
    protected DbSchema(String name, int version, String auth, TableSchema[] tables) {
        dbName = name;
        dbVersion = version;
        dbAuth = auth;
        dbTables = tables;
        
        for (TableSchema t : dbTables)
            t.init(this);
    }
    

    // ******************************************************************** //
    // Public Accessors.
    // ******************************************************************** //
    
    /**
     * Get the database name.
     * 
     * @return              The name of the database.
     */
    public String getDbName() {
        return dbName;
    }

    
    /**
     * Get the database version number.
     * 
     * @return              The database version number.
     */
    public int getDbVersion() {
        return dbVersion;
    }
    

    // ******************************************************************** //
    // Local Accessors.
    // ******************************************************************** //
    
    /**
     * Get the content provider authority string.
     * 
     * @return              The authority string.
     */
    String getDbAuth() {
        return dbAuth;
    }


    /**
     * Get the database table schemas.
     * 
     * @return              The table schemas.
     */
    TableSchema[] getDbTables() {
        return dbTables;
    }


    /**
     * Get the schema for a specified table.
     * 
     * @param   name            The name of the table we want.
     * @return                  The schema for the given table.
     * @throws  IllegalArgumentException  No such table.
     */
    protected TableSchema getTable(String name)
        throws IllegalArgumentException
    {
        for (TableSchema t : dbTables)
            if (t.getTableName().equals(name))
                return t;
        throw new IllegalArgumentException("No such table: " + name);
    }

    
	// ******************************************************************** //
	// Backup.
    // ******************************************************************** //

    public void backupDb(Context c, File where)
    	throws FileNotFoundException, IOException
    {
    	File bakDir = new File(where, dbName + ".bak");
    	if (!bakDir.isDirectory() && !bakDir.mkdirs())
    		throw new IOException("can't create backup dir " + bakDir);

		// Back up all the tables in the database.
    	ContentResolver cr = c.getContentResolver();
    	TableSchema[] tables = getDbTables();
    	for (TableSchema t : tables) {
    		FileOutputStream fos = null;
    		DataOutputStream dos = null;
    		try {
    	    	File bakFile = new File(bakDir, t.getTableName() + ".tb");
    			fos = new FileOutputStream(bakFile);
    			dos = new DataOutputStream(fos);

    			// Write a header containing a magic number, the backup format
    			// version, and the database schema version.
    			dos.writeInt(BACKUP_MAGIC);
    			dos.writeInt(BACKUP_VERSION);
    			dos.writeInt(dbVersion);

    			backupTable(cr, t, dos);
    		} finally {
    			if (dos != null) try {
    				dos.close();
    			} catch (IOException e) { }
    			if (fos != null) try {
    				fos.close();
    			} catch (IOException e) { }
    		}
    	}
    }


    private void backupTable(ContentResolver cr,
    						 TableSchema ts, DataOutputStream dos)
    	throws IOException
    {
    	Log.v(TAG, "BACKUP " + ts.getTableName());
    	
		// Create a where clause based on the backup mode.
		String where = null;
		String[] wargs = null;

		// Query for the records to back up.
		Cursor c = null;
		try {
			c = cr.query(ts.getContentUri(), ts.getDefaultProjection(),
						 where, wargs, ts.getSortOrder());
	    	Log.v(TAG, "==> " + c.getCount());
		
			// If there's no data, do nothing.
			if (!c.moveToFirst())
				return;
			
			// Get the column indices for all the columns.
			FieldDesc[] fields = ts.getTableFields();
			int[] cols = new int[fields.length];
			for (int i = 0; i < fields.length; ++i)
				cols[i] = c.getColumnIndex(fields[i].name);
			
			// Save all the rows.
			while (!c.isAfterLast()) {
				dos.writeInt(ROW_MAGIC);
				
				// Save all the fields in this row, each preceded by
				// its column number.
				StringBuilder sb1 = new StringBuilder(12);
				StringBuilder sb2 = new StringBuilder(120);
				for (int i = 0; i < fields.length; ++i) {
					TableSchema.FieldDesc fd = fields[i];
					TableSchema.FieldType ft = fd.type;

					// Skip absent fields.
					if (c.isNull(i) || (ft == TableSchema.FieldType.TEXT && c.getString(cols[i]) == null)) {
						sb1.append('_');
						continue;
					}
					
					sb1.append('x');
					sb2.append(" | " + fd.name + "=" + c.getString(cols[i]));
					dos.writeInt(i);
					switch (ft) {
					case _ID:
					case BIGINT:
						long lv = c.getLong(cols[i]);
						dos.writeLong(lv);
						break;
					case INT:
						int iv = c.getInt(cols[i]);
						dos.writeInt(iv);
						break;
					case DOUBLE:
					case FLOAT:
						double dv = c.getDouble(cols[i]);
						dos.writeDouble(dv);
						break;
					case REAL:
						float fv = c.getFloat(cols[i]);
						dos.writeFloat(fv);
						break;
					case BOOLEAN:
						boolean bv = c.getInt(cols[i]) != 0;
						dos.writeBoolean(bv);
						break;
					case TEXT:
						String sv = c.getString(cols[i]);
						dos.writeUTF(sv);
						break;
					}
				}
				Log.v(TAG, ">> " + sb1 + sb2);

				dos.writeInt(ROW_END);

				c.moveToNext();
			}
		} finally {
			c.close();
		}
	}

    
	// ******************************************************************** //
	// Restore.
    // ******************************************************************** //

    public void restoreDb(Context c, File where)
    	throws FileNotFoundException, IOException
    {
    	File bakDir = new File(where, dbName + ".bak");
    	if (!bakDir.isDirectory())
    		throw new IOException("can't find backup dir " + bakDir);

		// Back up all the tables in the database.
    	ContentResolver cr = c.getContentResolver();
    	TableSchema[] tables = getDbTables();
    	for (TableSchema t : tables) {
    		FileInputStream fis = null;
    		DataInputStream dis = null;
    		try {
    	    	File bakFile = new File(bakDir, t.getTableName() + ".tb");
    	    	if (!bakFile.isFile())
    	    		throw new IOException("can't find backup file " + bakFile);
    			fis = new FileInputStream(bakFile);
    			dis = new DataInputStream(fis);

    			// Write a header containing a magic number, the backup format
    			// version, and the database schema version.
    			checkInt(dis, BACKUP_MAGIC, "magic number", bakFile);
    			checkInt(dis, BACKUP_VERSION, "backup format version", bakFile);
    			checkInt(dis, dbVersion, "database schema version", bakFile);

    			wipeTable(cr, t);
    			restoreTable(cr, t, dis, bakFile);
    		} finally {
    			if (dis != null) try {
    				dis.close();
    			} catch (IOException e) { }
    			if (fis != null) try {
    				fis.close();
    			} catch (IOException e) { }
    		}
    	}
    }
    
    
    private void wipeTable(ContentResolver cr, TableSchema ts) {
    	Log.v(TAG, "WIPE " + ts.getTableName());
		cr.delete(ts.getContentUri(), null, null);
    }


    private void restoreTable(ContentResolver cr,
    						  TableSchema ts, DataInputStream dis, File bakFile)
    	throws IOException
    {
    	Log.v(TAG, "RESTORE " + ts.getTableName());
    	
    	// Get the column indices for all the columns.
    	FieldDesc[] fields = ts.getTableFields();

    	// Save all the rows.
    	ContentValues values = new ContentValues();
    	while (dis.available() > 0) {
			checkInt(dis, ROW_MAGIC, "row header", bakFile);
			
			values.clear();
	    	int i;
	    	while ((i = dis.readInt()) != ROW_END) {
	    		if (i < 0 || i >= fields.length)
    	    		throw new IOException("bad column number " + i +
    	    											" in " + bakFile);
    			TableSchema.FieldType t = fields[i].type;
    			switch (t) {
    			case _ID:
    			case BIGINT:
    				values.put(fields[i].name, dis.readLong());
    				break;
    			case INT:
    				values.put(fields[i].name, dis.readInt());
    				break;
    			case DOUBLE:
    			case FLOAT:
    				values.put(fields[i].name, dis.readDouble());
    				break;
    			case REAL:
    				values.put(fields[i].name, dis.readFloat());
    				break;
    			case BOOLEAN:
    				values.put(fields[i].name, dis.readBoolean());
    				break;
    			case TEXT:
    				values.put(fields[i].name, dis.readUTF());
    				break;
    			}
    		}

    		cr.insert(ts.getContentUri(), values);
    		dontInsert(values, fields);
    	}
	}
	

    private void checkInt(DataInputStream dis, int expect, String desc, File file)
    	throws IOException
    {
    	int actual = dis.readInt();
    	if (actual != expect)
    		throw new IOException("bad " + desc + " in " + file.getName() +
    							  ": expected 0x" + Integer.toHexString(expect) +
    							  "; got 0x" + Integer.toHexString(actual));
    }
    
    
    private void dontInsert(ContentValues values, FieldDesc[] fields) {
    	StringBuilder sb1 = new StringBuilder(12);
    	StringBuilder sb2 = new StringBuilder(120);
    	for (FieldDesc fd : fields) {
    		if (values.containsKey(fd.name)) {
    			sb1.append('x');
    			sb2.append(" | " + fd.name + "=" + values.getAsString(fd.name));
    		} else {
    			sb1.append('_');
    		}
    	}
    	Log.v(TAG, ">> " + sb1 + sb2);
    }
    

    // ******************************************************************** //
    // Class Data.
    // ******************************************************************** //

    // Debugging tag.
    static final String TAG = "DbSchema";
    

    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Magic number to identify backup files.
    private static final int BACKUP_MAGIC = 0x4d7e870a;

    // Version number of the backup file format.
    private static final int BACKUP_VERSION = 0x00010000;

    // Magic number to identify a row in a backup file.  This must be
    // distinct from any column number.
    private static final int ROW_MAGIC = 0xf5e782c3;

    // Magic number to identify the end of a row in a backup file.
    private static final int ROW_END = 0x82c3f5e7;


    // ******************************************************************** //
    // Private Data.
    // ******************************************************************** //

    // Database name and version.
    private final String dbName;
    private final int dbVersion;
    
    // Content provider authority.
    private final String dbAuth;
    
    // Definitions of our tables.
    private final TableSchema[] dbTables;
    
}

