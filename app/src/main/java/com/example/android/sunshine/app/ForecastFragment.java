
    package com.example.android.sunshine.app;


    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.database.Cursor;
    import android.net.Uri;
    import android.os.Bundle;
    import android.preference.PreferenceManager;
    import android.support.v4.app.Fragment;
    import android.support.v4.app.LoaderManager.LoaderCallbacks;
    import android.support.v4.content.CursorLoader;
    import android.support.v4.content.Loader;
    import android.util.Log;
    import android.view.LayoutInflater;
    import android.view.Menu;
    import android.view.MenuInflater;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.AdapterView;
    import android.widget.ListView;

    import com.example.android.sunshine.app.data.WeatherContract;
    import com.example.android.sunshine.app.data.WeatherContract.LocationEntry;
    import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;
    import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

    import java.util.Date;

    /**
     * Created by prateekjain on 9/11/15
     */

    public class ForecastFragment extends Fragment implements LoaderCallbacks<Cursor> {
      public static final String LOG_TAG = ForecastFragment.class.getSimpleName();
      private ForecastAdapter mForecastAdapter;

      private String mLocation;
      private ListView mListView;
      private int mPosition = ListView.INVALID_POSITION;
      private boolean mUseTodayLayout;
      private static final String SELECTED_KEY = "selected_position";
      private static final int FORECAST_LOADER = 0;
      private static final String[] FORECAST_COLUMNS = {
          WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
          WeatherEntry.COLUMN_DATETEXT,
          WeatherEntry.COLUMN_SHORT_DESC,
          WeatherEntry.COLUMN_MAX_TEMP,
          WeatherEntry.COLUMN_MIN_TEMP,
          LocationEntry.COLUMN_LOCATION_SETTING,
          WeatherEntry.COLUMN_WEATHER_ID,
          LocationEntry.COLUMN_COORD_LAT,
          LocationEntry.COLUMN_COORD_LONG
      };
      public static final int COL_WEATHER_ID = 0;
      public static final int COL_WEATHER_DATE = 1;
      public static final int COL_WEATHER_DESC = 2;
      public static final int COL_WEATHER_MAX_TEMP = 3;
      public static final int COL_WEATHER_MIN_TEMP = 4;
      public static final int COL_LOCATION_SETTING = 5;
      public static final int COL_WEATHER_CONDITION_ID = 6;
      public static final int COL_COORD_LAT = 7;
      public static final int COL_COORD_LONG = 8;
      public interface Callback {

        public void onItemSelected(String date);
      }

      public ForecastFragment() {
      }

      @Override
      public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
      }

      @Override
      public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.forecastfragment, menu);
      }

      @Override
      public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_refresh) {
            updateWeather();
            return true;
        }
        if (id == R.id.action_map) {
          openPreferredLocationInMap();
          return true;
        }

        return super.onOptionsItemSelected(item);
      }

      @Override
      public View onCreateView(LayoutInflater inflater, ViewGroup container,
                               Bundle savedInstanceState) {

        // The ArrayAdapter will take data from a source and
        // use it to populate the ListView it's attached to.
        mForecastAdapter = new ForecastAdapter(getActivity(), null, 0);

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        // Get a reference to the ListView, and attach this adapter to it.
        mListView = (ListView) rootView.findViewById(R.id.listview_forecast);
        mListView.setAdapter(mForecastAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

          @Override
          public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
            Cursor cursor = mForecastAdapter.getCursor();
            if (cursor != null && cursor.moveToPosition(position)) {
              ((Callback)getActivity())
                  .onItemSelected(cursor.getString(COL_WEATHER_DATE));
            }
            mPosition = position;
          }
        });
        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_KEY)) {
          mPosition = savedInstanceState.getInt(SELECTED_KEY);
        }

        mForecastAdapter.setUseTodayLayout(mUseTodayLayout);

        return rootView;
      }

      @Override
      public void onActivityCreated(Bundle savedInstanceState) {
        getLoaderManager().initLoader(FORECAST_LOADER, null, this);
        super.onActivityCreated(savedInstanceState);
      }

      private void updateWeather() {
        SunshineSyncAdapter.syncImmediately(getActivity());
      }

      private void openPreferredLocationInMap() {
        if ( null != mForecastAdapter ) {
          Cursor c = mForecastAdapter.getCursor();
          if ( null != c ) {
            c.moveToPosition(0);
            String posLat = c.getString(COL_COORD_LAT);
            String posLong = c.getString(COL_COORD_LONG);
            Uri geoLocation = Uri.parse("geo:" + posLat + "," + posLong);

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(geoLocation);

            if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
              startActivity(intent);
            } else {
              Log.d(LOG_TAG, "Couldn't call " + geoLocation.toString() + ", no receiving apps installed!");
            }
          }

        }
      }

      @Override
      public void onResume() {
        super.onResume();
        if (mLocation != null && !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
          getLoaderManager().restartLoader(FORECAST_LOADER, null, this);
        }
      }

      @Override
      public void onSaveInstanceState(Bundle outState) {
        if (mPosition != ListView.INVALID_POSITION) {
          outState.putInt(SELECTED_KEY, mPosition);
        }
        super.onSaveInstanceState(outState);
      }

      @Override
      public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String startDate = WeatherContract.getDbDateString(new Date());
        String sortOrder = WeatherEntry.COLUMN_DATETEXT + " ASC";

        mLocation = Utility.getPreferredLocation(getActivity());
        Uri weatherForLocationUri = WeatherEntry.buildWeatherLocationWithStartDate(
            mLocation, startDate);
        return new CursorLoader(
            getActivity(),
            weatherForLocationUri,
            FORECAST_COLUMNS,
            null,
            null,
            sortOrder
        );
      }

      @Override
      public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        mForecastAdapter.swapCursor(data);
        if (mPosition != ListView.INVALID_POSITION) {
          mListView.smoothScrollToPosition(mPosition);
        }
      }

      @Override
      public void onLoaderReset(Loader<Cursor> loader) {
        mForecastAdapter.swapCursor(null);
      }

      public void setUseTodayLayout(boolean useTodayLayout) {
        mUseTodayLayout = useTodayLayout;
        if (mForecastAdapter != null) {
          mForecastAdapter.setUseTodayLayout(mUseTodayLayout);
        }
      }
    }