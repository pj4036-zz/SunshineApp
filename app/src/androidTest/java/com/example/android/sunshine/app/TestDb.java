package com.example.android.sunshine.app;

/**
 * Created by prateekjain on 8/14/15.
 */
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.data.WeatherDbHelper;

import java.util.Map;
import java.util.Set;

public class TestDb extends AndroidTestCase {

  public static final String LOG_TAG = TestDb.class.getSimpleName();
  static final String TEST_LOCATION = "99705";
  static final String TEST_DATE = "20141205";

  public void testCreateDb() throws Throwable {
    mContext.deleteDatabase(WeatherDbHelper.DATABASE_NAME);
    SQLiteDatabase db = new WeatherDbHelper(
        this.mContext).getWritableDatabase();
    assertEquals(true, db.isOpen());
    db.close();
  }

  public void testInsertReadDb() {
    WeatherDbHelper dbHelper = new WeatherDbHelper(mContext);
    SQLiteDatabase db = dbHelper.getWritableDatabase();

    ContentValues testValues = createNorthPoleLocationValues();

    long locationRowId;
    locationRowId = db.insert(LocationEntry.TABLE_NAME, null, testValues);
    assertTrue(locationRowId != -1);
    Log.d(LOG_TAG, "New row id: " + locationRowId);
    Cursor cursor = db.query(
        LocationEntry.TABLE_NAME,  // Table to Query
        null, // all columns
        null, // Columns for the "where" clause
        null, // Values for the "where" clause
        null, // columns to group by
        null, // columns to filter by row groups
        null // sort order
    );

    validateCursor(cursor, testValues);
    ContentValues weatherValues = createWeatherValues(locationRowId);
    long weatherRowId = db.insert(WeatherEntry.TABLE_NAME, null, weatherValues);
    assertTrue(weatherRowId != -1);
    Cursor weatherCursor = db.query(WeatherEntry.TABLE_NAME, null, null, null, null, null, null);
    validateCursor(weatherCursor, weatherValues);
    dbHelper.close();
  }

  static ContentValues createWeatherValues(long locationRowId) {
    ContentValues weatherValues = new ContentValues();
    weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);
    weatherValues.put(WeatherEntry.COLUMN_DATETEXT, TEST_DATE);
    weatherValues.put(WeatherEntry.COLUMN_DEGREES, 1.1);
    weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, 1.2);
    weatherValues.put(WeatherEntry.COLUMN_PRESSURE, 1.3);
    weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, 75);
    weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, 65);
    weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, "Asteroids");
    weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, 5.5);
    weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, 321);

    return weatherValues;
  }

  static ContentValues createNorthPoleLocationValues() {
    ContentValues testValues = new ContentValues();
    testValues.put(LocationEntry.COLUMN_LOCATION_SETTING, TEST_LOCATION);
    testValues.put(LocationEntry.COLUMN_CITY_NAME, "North Pole");
    testValues.put(LocationEntry.COLUMN_COORD_LAT, 64.7488);
    testValues.put(LocationEntry.COLUMN_COORD_LONG, -147.353);

    return testValues;
  }
  static void validateCursor(Cursor valueCursor, ContentValues expectedValues) {
    assertTrue(valueCursor.moveToFirst());
    Set<Map.Entry<String, Object>> valueSet = expectedValues.valueSet();
    for (Map.Entry<String, Object> entry : valueSet) {
      String columnName = entry.getKey();
      int idx = valueCursor.getColumnIndex(columnName);
      assertFalse(idx == -1);
      String expectedValue = entry.getValue().toString();
      assertEquals(expectedValue, valueCursor.getString(idx));
    }
    valueCursor.close();
  }
}
