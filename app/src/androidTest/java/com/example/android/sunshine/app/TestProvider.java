package com.example.android.sunshine.app;

/**
 * Created by prateekjain on 8/14/15.
 */
import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.test.AndroidTestCase;
import android.util.Log;

import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

public class TestProvider extends AndroidTestCase {

  public static final String LOG_TAG = TestProvider.class.getSimpleName();
  public void deleteAllRecords() {
    mContext.getContentResolver().delete(WeatherEntry.CONTENT_URI, null, null);
    mContext.getContentResolver().delete(LocationEntry.CONTENT_URI, null, null);
    Cursor cursor = mContext.getContentResolver().query(WeatherEntry.CONTENT_URI, null, null, null, null);
    assertEquals(0, cursor.getCount());
    cursor.close();
    cursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI, null, null, null, null);
    assertEquals(0, cursor.getCount());
    cursor.close();
  }
  public void setUp() {
    deleteAllRecords();
  }
  public void testInsertReadProvider() {
    ContentValues testValues = TestDb.createNorthPoleLocationValues();
    Uri locationUri = mContext.getContentResolver().insert(LocationEntry.CONTENT_URI, testValues);
    long locationRowId = ContentUris.parseId(locationUri);
    assertTrue(locationRowId != -1);
    Cursor cursor = mContext.getContentResolver().query(LocationEntry.CONTENT_URI, null, null, null, null);
    TestDb.validateCursor(cursor, testValues);
    cursor = mContext.getContentResolver().query(LocationEntry.buildLocationUri(locationRowId), null, null, null, null);
    TestDb.validateCursor(cursor, testValues);
    ContentValues weatherValues = TestDb.createWeatherValues(locationRowId);
    Uri weatherInsertUri = mContext.getContentResolver().insert(WeatherEntry.CONTENT_URI, weatherValues);
    assertTrue(weatherInsertUri != null);
    Cursor weatherCursor = mContext.getContentResolver().query(WeatherEntry.CONTENT_URI, null, null, null, null);
    TestDb.validateCursor(weatherCursor, weatherValues);
    addAllContentValues(weatherValues, testValues);
    weatherCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocation(TestDb.TEST_LOCATION), null, null, null, null);
    TestDb.validateCursor(weatherCursor, weatherValues);
    weatherCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocationWithStartDate(TestDb.TEST_LOCATION, TestDb.TEST_DATE), null, null, null, null);
    TestDb.validateCursor(weatherCursor, weatherValues);
    weatherCursor = mContext.getContentResolver().query(WeatherEntry.buildWeatherLocationWithDate(TestDb.TEST_LOCATION, TestDb.TEST_DATE), null, null, null, null);
    TestDb.validateCursor(weatherCursor, weatherValues);
  }

  public void testGetType() {
    String type = mContext.getContentResolver().getType(WeatherEntry.CONTENT_URI);
    assertEquals(WeatherEntry.CONTENT_TYPE, type);
    String testLocation = "94074";
    type = mContext.getContentResolver().getType(
        WeatherEntry.buildWeatherLocation(testLocation));
    assertEquals(WeatherEntry.CONTENT_TYPE, type);
    String testDate = "20140612";
    type = mContext.getContentResolver().getType(
        WeatherEntry.buildWeatherLocationWithDate(testLocation, testDate));
    assertEquals(WeatherEntry.CONTENT_ITEM_TYPE, type);
    type = mContext.getContentResolver().getType(LocationEntry.CONTENT_URI);
    assertEquals(LocationEntry.CONTENT_TYPE, type);
    type = mContext.getContentResolver().getType(LocationEntry.buildLocationUri(1L));
    assertEquals(LocationEntry.CONTENT_ITEM_TYPE, type);
  }

  public void testUpdateLocation() {
    ContentValues values = TestDb.createNorthPoleLocationValues();
    Uri locationUri = mContext.getContentResolver().
        insert(LocationEntry.CONTENT_URI, values);
    long locationRowId = ContentUris.parseId(locationUri);
    assertTrue(locationRowId != -1);
    Log.d(LOG_TAG, "New row id: " + locationRowId);
    ContentValues updatedValues = new ContentValues(values);
    updatedValues.put(LocationEntry._ID, locationRowId);
    updatedValues.put(LocationEntry.COLUMN_CITY_NAME, "Santa's Village");
    int count = mContext.getContentResolver().update(
        LocationEntry.CONTENT_URI, updatedValues, LocationEntry._ID + "= ?",
        new String[] { Long.toString(locationRowId)});
    assertEquals(count, 1);
    Cursor cursor = mContext.getContentResolver().query(LocationEntry.buildLocationUri(locationRowId), null, null, null, null);
    TestDb.validateCursor(cursor, updatedValues);
  }
  public void testDeleteRecordsAtEnd() {
    deleteAllRecords();
  }
  @TargetApi(Build.VERSION_CODES.HONEYCOMB)
  void addAllContentValues(ContentValues destination, ContentValues source) {
    for (String key : source.keySet()) {
      destination.put(key, source.getAsString(key));
    }
  }

  static final String KALAMAZOO_LOCATION_SETTING = "kalamazoo";
  static final String KALAMAZOO_WEATHER_START_DATE = "20140625";

  long locationRowId;

  static ContentValues createKalamazooWeatherValues(long locationRowId) {
    ContentValues weatherValues = new ContentValues();
    weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationRowId);
    weatherValues.put(WeatherEntry.COLUMN_DATETEXT, KALAMAZOO_WEATHER_START_DATE);
    weatherValues.put(WeatherEntry.COLUMN_DEGREES, 1.2);
    weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, 1.5);
    weatherValues.put(WeatherEntry.COLUMN_PRESSURE, 1.1);
    weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, 85);
    weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, 35);
    weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, "Cats and Dogs");
    weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, 3.4);
    weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, 42);

    return weatherValues;
  }

  static ContentValues createKalamazooLocationValues() {
    ContentValues testValues = new ContentValues();
    testValues.put(LocationEntry.COLUMN_LOCATION_SETTING, KALAMAZOO_LOCATION_SETTING);
    testValues.put(LocationEntry.COLUMN_CITY_NAME, "Kalamazoo");
    testValues.put(LocationEntry.COLUMN_COORD_LAT, 42.2917);
    testValues.put(LocationEntry.COLUMN_COORD_LONG, -85.5872);

    return testValues;
  }
  public void insertKalamazooData() {
    ContentValues kalamazooLocationValues = createKalamazooLocationValues();
    Uri locationInsertUri = mContext.getContentResolver()
        .insert(LocationEntry.CONTENT_URI, kalamazooLocationValues);
    assertTrue(locationInsertUri != null);
    locationRowId = ContentUris.parseId(locationInsertUri);
    ContentValues kalamazooWeatherValues = createKalamazooWeatherValues(locationRowId);
    Uri weatherInsertUri = mContext.getContentResolver()
        .insert(WeatherEntry.CONTENT_URI, kalamazooWeatherValues);
    assertTrue(weatherInsertUri != null);
  }

  public void testUpdateAndReadWeather() {
    insertKalamazooData();
    String newDescription = "Cats and Frogs (don't warn the tadpoles!)";
    ContentValues kalamazooUpdate = new ContentValues();
    kalamazooUpdate.put(WeatherEntry.COLUMN_SHORT_DESC, newDescription);
    mContext.getContentResolver().update(
        WeatherEntry.CONTENT_URI, kalamazooUpdate, null, null);
    Cursor weatherCursor = mContext.getContentResolver().query(
        WeatherEntry.CONTENT_URI,
        null,
        null,
        null,
        null
    );
    ContentValues kalamazooAltered = createKalamazooWeatherValues(locationRowId);
    kalamazooAltered.put(WeatherEntry.COLUMN_SHORT_DESC, newDescription);

    TestDb.validateCursor(weatherCursor, kalamazooAltered);
  }

  public void testRemoveHumidityAndReadWeather() {
    insertKalamazooData();

    mContext.getContentResolver().delete(WeatherEntry.CONTENT_URI,
        WeatherEntry.COLUMN_HUMIDITY + " = " + locationRowId, null);
    Cursor weatherCursor = mContext.getContentResolver().query(WeatherEntry.CONTENT_URI, null, null, null, null);
    ContentValues kalamazooAltered = createKalamazooWeatherValues(locationRowId);
    kalamazooAltered.remove(WeatherEntry.COLUMN_HUMIDITY);

    TestDb.validateCursor(weatherCursor, kalamazooAltered);
    int idx = weatherCursor.getColumnIndex(WeatherEntry.COLUMN_HUMIDITY);
    assertEquals(-1, idx);
  }
}
