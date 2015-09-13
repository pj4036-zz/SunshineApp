package com.example.android.sunshine.app.sync;

/**
 * Created by prateekjain on 9/14/15.
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SyncRequest;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;

import com.example.android.sunshine.app.MainActivity;
import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.Utility;
import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Vector;

public class SunshineSyncAdapter extends AbstractThreadedSyncAdapter {
  public final String LOG_TAG = SunshineSyncAdapter.class.getSimpleName();
  public static final int SYNC_INTERVAL = 60 * 180;
  public static final int SYNC_FLEXTIME = SYNC_INTERVAL/3;
  private static final long DAY_IN_MILLIS = 1000 * 60 * 60 * 24;
  private static final int WEATHER_NOTIFICATION_ID = 3004;


  private static final String[] NOTIFY_WEATHER_PROJECTION = new String[] {
      WeatherEntry.COLUMN_WEATHER_ID,
      WeatherEntry.COLUMN_MAX_TEMP,
      WeatherEntry.COLUMN_MIN_TEMP,
      WeatherEntry.COLUMN_SHORT_DESC
  };
  private static final int INDEX_WEATHER_ID = 0;
  private static final int INDEX_MAX_TEMP = 1;
  private static final int INDEX_MIN_TEMP = 2;
  private static final int INDEX_SHORT_DESC = 3;

  public SunshineSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider, SyncResult syncResult) {
    Log.d(LOG_TAG, "Starting sync");
    String locationQuery = Utility.getPreferredLocation(getContext());
    HttpURLConnection urlConnection = null;
    BufferedReader reader = null;

    String forecastJsonStr = null;

    String format = "json";
    String units = "metric";
    int numDays = 14;

    try {
      final String FORECAST_BASE_URL =
          "http://api.openweathermap.org/data/2.5/forecast/daily?";
      final String QUERY_PARAM = "q";
      final String FORMAT_PARAM = "mode";
      final String UNITS_PARAM = "units";
      final String DAYS_PARAM = "cnt";

      Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon().appendQueryParameter(QUERY_PARAM, locationQuery).appendQueryParameter(FORMAT_PARAM, format).appendQueryParameter(UNITS_PARAM, units).appendQueryParameter(DAYS_PARAM, Integer.toString(numDays)).build();
      URL url = new URL(builtUri.toString());
      urlConnection = (HttpURLConnection) url.openConnection();
      urlConnection.setRequestMethod("GET");
      urlConnection.connect();
      InputStream inputStream = urlConnection.getInputStream();
      StringBuffer buffer = new StringBuffer();
      if (inputStream == null) {
        return;
      }

      reader = new BufferedReader(new InputStreamReader(inputStream));
      String line;
      while ((line = reader.readLine()) != null) {
        buffer.append(line + "\n");
      }

      if (buffer.length() == 0) {
        return;
      }
      forecastJsonStr = buffer.toString();
    } catch (IOException e) {
      Log.e(LOG_TAG, "Error ", e);
      return;
    } finally {
      if (urlConnection != null) {
        urlConnection.disconnect();
      }
      if (reader != null) {
        try {
          reader.close();
        } catch (final IOException e) {
          Log.e(LOG_TAG, "Error closing stream", e);
        }
      }
    }
    final String OWM_CITY = "city";
    final String OWM_CITY_NAME = "name";
    final String OWM_COORD = "coord";
    final String OWM_LATITUDE = "lat";
    final String OWM_LONGITUDE = "lon";
    final String OWM_LIST = "list";
    final String OWM_DATETIME = "dt";
    final String OWM_PRESSURE = "pressure";
    final String OWM_HUMIDITY = "humidity";
    final String OWM_WINDSPEED = "speed";
    final String OWM_WIND_DIRECTION = "deg";
    final String OWM_TEMPERATURE = "temp";
    final String OWM_MAX = "max";
    final String OWM_MIN = "min";
    final String OWM_WEATHER = "weather";
    final String OWM_DESCRIPTION = "main";
    final String OWM_WEATHER_ID = "id";

    try {
      JSONObject forecastJson = new JSONObject(forecastJsonStr);
      JSONArray weatherArray = forecastJson.getJSONArray(OWM_LIST);
      JSONObject cityJson = forecastJson.getJSONObject(OWM_CITY);
      String cityName = cityJson.getString(OWM_CITY_NAME);
      JSONObject cityCoord = cityJson.getJSONObject(OWM_COORD);
      double cityLatitude = cityCoord.getDouble(OWM_LATITUDE);
      double cityLongitude = cityCoord.getDouble(OWM_LONGITUDE);
      long locationId = addLocation(locationQuery, cityName, cityLatitude, cityLongitude);
      Vector<ContentValues> cVVector = new Vector<ContentValues>(weatherArray.length());
      for(int i = 0; i < weatherArray.length(); i++) {
        long dateTime;
        double pressure;
        int humidity;
        double windSpeed;
        double windDirection;
        double high;
        double low;
        String description;
        int weatherId;
        JSONObject dayForecast = weatherArray.getJSONObject(i);
        dateTime = dayForecast.getLong(OWM_DATETIME);
        pressure = dayForecast.getDouble(OWM_PRESSURE);
        humidity = dayForecast.getInt(OWM_HUMIDITY);
        windSpeed = dayForecast.getDouble(OWM_WINDSPEED);
        windDirection = dayForecast.getDouble(OWM_WIND_DIRECTION);
        JSONObject weatherObject =
            dayForecast.getJSONArray(OWM_WEATHER).getJSONObject(0);
        description = weatherObject.getString(OWM_DESCRIPTION);
        weatherId = weatherObject.getInt(OWM_WEATHER_ID);
        JSONObject temperatureObject = dayForecast.getJSONObject(OWM_TEMPERATURE);
        high = temperatureObject.getDouble(OWM_MAX);
        low = temperatureObject.getDouble(OWM_MIN);
        ContentValues weatherValues = new ContentValues();
        weatherValues.put(WeatherEntry.COLUMN_LOC_KEY, locationId);
        weatherValues.put(WeatherEntry.COLUMN_DATETEXT, WeatherContract.getDbDateString(new Date(dateTime * 1000L)));
        weatherValues.put(WeatherEntry.COLUMN_HUMIDITY, humidity);
        weatherValues.put(WeatherEntry.COLUMN_PRESSURE, pressure);
        weatherValues.put(WeatherEntry.COLUMN_WIND_SPEED, windSpeed);
        weatherValues.put(WeatherEntry.COLUMN_DEGREES, windDirection);
        weatherValues.put(WeatherEntry.COLUMN_MAX_TEMP, high);
        weatherValues.put(WeatherEntry.COLUMN_MIN_TEMP, low);
        weatherValues.put(WeatherEntry.COLUMN_SHORT_DESC, description);
        weatherValues.put(WeatherEntry.COLUMN_WEATHER_ID, weatherId);
        cVVector.add(weatherValues);
      }
      if ( cVVector.size() > 0 ) {
        ContentValues[] cvArray = new ContentValues[cVVector.size()];
        cVVector.toArray(cvArray);
        getContext().getContentResolver().bulkInsert(WeatherEntry.CONTENT_URI, cvArray);

        Calendar cal = Calendar.getInstance(); //Get's a calendar object with the current time.
        cal.add(Calendar.DATE, -1); //Signifies yesterday's date
        String yesterdayDate = WeatherContract.getDbDateString(cal.getTime());
        getContext().getContentResolver().delete(WeatherEntry.CONTENT_URI,
            WeatherEntry.COLUMN_DATETEXT + " <= ?",
            new String[] {yesterdayDate});

        notifyWeather();
      }
      Log.d(LOG_TAG, "FetchWeatherTask Complete. " + cVVector.size() + " Inserted");

    } catch (JSONException e) {
      Log.e(LOG_TAG, e.getMessage(), e);
      e.printStackTrace();
    }
    return;
  }


  private void notifyWeather() {
    Context context = getContext();
    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
    String displayNotificationsKey = context.getString(R.string.pref_enable_notifications_key);
    boolean displayNotifications = prefs.getBoolean(displayNotificationsKey,
        Boolean.parseBoolean(context.getString(R.string.pref_enable_notifications_default)));

    if ( displayNotifications ) {

      String lastNotificationKey = context.getString(R.string.pref_last_notification);
      long lastSync = prefs.getLong(lastNotificationKey, 0);

      if (System.currentTimeMillis() - lastSync >= DAY_IN_MILLIS) {
        String locationQuery = Utility.getPreferredLocation(context);
        Uri weatherUri = WeatherEntry.buildWeatherLocationWithDate(locationQuery, WeatherContract.getDbDateString(new Date()));
        Cursor cursor = context.getContentResolver().query(weatherUri, NOTIFY_WEATHER_PROJECTION, null, null, null);

        if (cursor.moveToFirst()) {
          int weatherId = cursor.getInt(INDEX_WEATHER_ID);
          double high = cursor.getDouble(INDEX_MAX_TEMP);
          double low = cursor.getDouble(INDEX_MIN_TEMP);
          String desc = cursor.getString(INDEX_SHORT_DESC);
          int iconId = Utility.getIconResourceForWeatherCondition(weatherId);
          String title = context.getString(R.string.app_name);
          String contentText = String.format(context.getString(R.string.format_notification),
              desc,
              Utility.formatTemperature(context, high),
              Utility.formatTemperature(context, low));
          NotificationCompat.Builder mBuilder =
              new NotificationCompat.Builder(getContext())
                  .setSmallIcon(iconId)
                  .setContentTitle(title)
                  .setContentText(contentText);
          Intent resultIntent = new Intent(context, MainActivity.class);
          TaskStackBuilder stackBuilder = TaskStackBuilder.create(context);
          stackBuilder.addNextIntent(resultIntent);
          PendingIntent resultPendingIntent =
              stackBuilder.getPendingIntent(
                  0,
                  PendingIntent.FLAG_UPDATE_CURRENT
              );
          mBuilder.setContentIntent(resultPendingIntent);

          NotificationManager mNotificationManager =
              (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
          mNotificationManager.notify(WEATHER_NOTIFICATION_ID, mBuilder.build());
          SharedPreferences.Editor editor = prefs.edit();
          editor.putLong(lastNotificationKey, System.currentTimeMillis());
          editor.commit();
        }
      }
    }

  }
  private long addLocation(String locationSetting, String cityName, double lat, double lon) {
    long locationId;

    Log.v(LOG_TAG, "inserting " + cityName + ", with coord: " + lat + ", " + lon);
    Cursor locationCursor = getContext().getContentResolver().query(
        WeatherContract.LocationEntry.CONTENT_URI,
        new String[]{LocationEntry._ID},
        LocationEntry.COLUMN_LOCATION_SETTING + " = ?",
        new String[]{locationSetting},
        null);

    if (locationCursor.moveToFirst()) {
      int locationIdIndex = locationCursor.getColumnIndex(LocationEntry._ID);
      locationId = locationCursor.getLong(locationIdIndex);
    } else {
      ContentValues locationValues = new ContentValues();
      locationValues.put(LocationEntry.COLUMN_CITY_NAME, cityName);
      locationValues.put(LocationEntry.COLUMN_LOCATION_SETTING, locationSetting);
      locationValues.put(LocationEntry.COLUMN_COORD_LAT, lat);
      locationValues.put(LocationEntry.COLUMN_COORD_LONG, lon);
      Uri insertedUri = getContext().getContentResolver().insert(
          WeatherContract.LocationEntry.CONTENT_URI,
          locationValues
      );
      locationId = ContentUris.parseId(insertedUri);
    }
    return locationId;
  }
  public static void configurePeriodicSync(Context context, int syncInterval, int flexTime) {
    Account account = getSyncAccount(context);
    String authority = context.getString(R.string.content_authority);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      SyncRequest request = new SyncRequest.Builder().
          syncPeriodic(syncInterval, flexTime).
          setSyncAdapter(account, authority).build();
      ContentResolver.requestSync(request);
    } else {
      ContentResolver.addPeriodicSync(account,
          authority, new Bundle(), syncInterval);
    }
  }
  public static void syncImmediately(Context context) {
    Bundle bundle = new Bundle();
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
    bundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
    ContentResolver.requestSync(getSyncAccount(context),
        context.getString(R.string.content_authority), bundle);
  }
  public static Account getSyncAccount(Context context) {
    AccountManager accountManager =
        (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
    Account newAccount = new Account(
        context.getString(R.string.app_name), context.getString(R.string.sync_account_type));
    if ( null == accountManager.getPassword(newAccount) ) {
      if (!accountManager.addAccountExplicitly(newAccount, "", null)) {
        return null;
      }
      onAccountCreated(newAccount, context);
    }
    return newAccount;
  }


  private static void onAccountCreated(Account newAccount, Context context) {
    SunshineSyncAdapter.configurePeriodicSync(context, SYNC_INTERVAL, SYNC_FLEXTIME);
    ContentResolver.setSyncAutomatically(newAccount, context.getString(R.string.content_authority), true);
    syncImmediately(context);
  }

  public static void initializeSyncAdapter(Context context) {
    getSyncAccount(context);
  }


}