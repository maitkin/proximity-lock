package com.m3sys.proxlock.db;

/**
 Proximity-Lock
 Copyright (C) 2014  Marc W. Aitkin

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.

 */

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class DataBridge {

  protected static final String BASE_TAG = "m3sys.proxlock";
  private static final String TAG = BASE_TAG + "-DataBridge";
  // Database fields
  private SQLiteDatabase database;
  private DbHelper dbHelper;


  public DataBridge(Context context) {
    dbHelper = new DbHelper(context);
  }

  public void open() throws SQLException {
    database = dbHelper.getWritableDatabase();
  }

  public void close() {
    dbHelper.close();
  }

  public PLocation insertLocation(String name, double lat, double lon, boolean safe, int type) {

    ContentValues values = new ContentValues();

    values.put(DbHelper.COLUMN_NAME,name);
    values.put(DbHelper.COLUMN_LATITUDE, lat);
    values.put(DbHelper.COLUMN_LONGITUDE, lon);
    values.put(DbHelper.COLUMN_SAFE, safe ? 1 : 0);
    values.put(DbHelper.COLUMN_TYPE, type);

    long insertId = database.insert(DbHelper.TABLE_LOCATIONS, null, values);

    Cursor cursor = database.query(DbHelper.TABLE_LOCATIONS,DbHelper.allColumns,
            DbHelper.COLUMN_ID + " = " + insertId, null, null, null, null);

    cursor.moveToFirst();
    PLocation newLocation = cursorToLocation(cursor);
    cursor.close();
    return newLocation;
  }


  public void deleteLocation(long id) {

    System.out.println("Deleting id: " + id);
    database.delete(DbHelper.TABLE_LOCATIONS, DbHelper.COLUMN_ID + " = " + id, null);
  }

  public Cursor allLocations() {
      return  database.query(DbHelper.TABLE_LOCATIONS,
              DbHelper.allColumns, null, null, null, null, null);
  }
  public List<PLocation> getAllLocations() {
    List<PLocation> locations = new ArrayList<PLocation>();

    Cursor cursor = database.query(DbHelper.TABLE_LOCATIONS,
        DbHelper.allColumns, null, null, null, null, null);

    cursor.moveToFirst();
    while (!cursor.isAfterLast()) {
      PLocation location = cursorToLocation(cursor);
      locations.add(location);
      cursor.moveToNext();
    }
    // make sure to close the cursor
    cursor.close();
    return locations;
  }

  private PLocation cursorToLocation(Cursor cursor) {
    PLocation Location = new PLocation();
    Location.setId(cursor.getLong(0));
    Location.setName(cursor.getString(1));
    Location.setLatitude(cursor.getDouble(2));
    Location.setLongitude(cursor.getDouble(3));
    Location.setSafe(cursor.getInt(4) == 1);
    Location.setType(cursor.getInt(5));
    return Location;
  }
}
