package com.example.android.sunshine.app;

/**
 * Created by prateekjain on 9/14/15.
 */
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment; import android.support.v4.app.LoaderManager;
    import android.support.v4.content.CursorLoader;
    import android.support.v4.content.Loader;
    import android.support.v4.view.MenuItemCompat;
    import android.support.v7.widget.ShareActionProvider;
    import android.view.LayoutInflater;
    import android.view.Menu;
    import android.view.MenuInflater;
    import android.view.MenuItem;
    import android.view.View;
    import android.view.ViewGroup;
    import android.widget.ImageView;
    import android.widget.TextView;

    import com.example.android.sunshine.app.data.WeatherContract;
    import com.example.android.sunshine.app.data.WeatherContract.WeatherEntry;

/**
 * A placeholder fragment containing a simple view.
 */
public class DetailFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

  private static final String LOG_TAG = DetailFragment.class.getSimpleName();

  private static final String FORECAST_SHARE_HASHTAG = " #SunshineApp";

  private static final String LOCATION_KEY = "location";

  private ShareActionProvider mShareActionProvider;
  private String mLocation;
  private String mForecast;
  private String mDateStr;

  private static final int DETAIL_LOADER = 0;

  private static final String[] FORECAST_COLUMNS = {
      WeatherEntry.TABLE_NAME + "." + WeatherEntry._ID,
      WeatherEntry.COLUMN_DATETEXT,
      WeatherEntry.COLUMN_SHORT_DESC,
      WeatherEntry.COLUMN_MAX_TEMP,
      WeatherEntry.COLUMN_MIN_TEMP,
      WeatherEntry.COLUMN_HUMIDITY,
      WeatherEntry.COLUMN_PRESSURE,
      WeatherEntry.COLUMN_WIND_SPEED,
      WeatherEntry.COLUMN_DEGREES,
      WeatherEntry.COLUMN_WEATHER_ID,

      WeatherContract.LocationEntry.COLUMN_LOCATION_SETTING
  };

  private ImageView mIconView;
  private TextView mFriendlyDateView;
  private TextView mDateView;
  private TextView mDescriptionView;
  private TextView mHighTempView;
  private TextView mLowTempView;
  private TextView mHumidityView;
  private TextView mWindView;
  private TextView mPressureView;

  public DetailFragment() {
    setHasOptionsMenu(true);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    outState.putString(LOCATION_KEY, mLocation);
    super.onSaveInstanceState(outState);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {

    Bundle arguments = getArguments();
    if (arguments != null) {
      mDateStr = arguments.getString(DetailActivity.DATE_KEY);
    }

    if (savedInstanceState != null) {
      mLocation = savedInstanceState.getString(LOCATION_KEY);
    }

    View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
    mIconView = (ImageView) rootView.findViewById(R.id.detail_icon);
    mDateView = (TextView) rootView.findViewById(R.id.detail_date_textview);
    mFriendlyDateView = (TextView) rootView.findViewById(R.id.detail_day_textview);
    mDescriptionView = (TextView) rootView.findViewById(R.id.detail_forecast_textview);
    mHighTempView = (TextView) rootView.findViewById(R.id.detail_high_textview);
    mLowTempView = (TextView) rootView.findViewById(R.id.detail_low_textview);
    mHumidityView = (TextView) rootView.findViewById(R.id.detail_humidity_textview);
    mWindView = (TextView) rootView.findViewById(R.id.detail_wind_textview);
    mPressureView = (TextView) rootView.findViewById(R.id.detail_pressure_textview);
    return rootView;
  }

  @Override
  public void onResume() {
    super.onResume();
    Bundle arguments = getArguments();
    if (arguments != null && arguments.containsKey(DetailActivity.DATE_KEY) &&
        mLocation != null &&
        !mLocation.equals(Utility.getPreferredLocation(getActivity()))) {
      getLoaderManager().restartLoader(DETAIL_LOADER, null, this);
    }
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.detailfragment, menu);
    MenuItem menuItem = menu.findItem(R.id.action_share);
    mShareActionProvider = (ShareActionProvider) MenuItemCompat.getActionProvider(menuItem);
    if (mForecast != null) {
      mShareActionProvider.setShareIntent(createShareForecastIntent());
    }
  }

  private Intent createShareForecastIntent() {
    Intent shareIntent = new Intent(Intent.ACTION_SEND);
    shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    shareIntent.setType("text/plain");
    shareIntent.putExtra(Intent.EXTRA_TEXT, mForecast + FORECAST_SHARE_HASHTAG);
    return shareIntent;
  }

  @Override
  public void onActivityCreated(Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);
    if (savedInstanceState != null) {
      mLocation = savedInstanceState.getString(LOCATION_KEY);
    }

    Bundle arguments = getArguments();
    if (arguments != null && arguments.containsKey(DetailActivity.DATE_KEY)) {
      getLoaderManager().initLoader(DETAIL_LOADER, null, this);
    }
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    String sortOrder = WeatherContract.WeatherEntry.COLUMN_DATETEXT + " ASC";

    mLocation = Utility.getPreferredLocation(getActivity());
    Uri weatherForLocationUri = WeatherContract.WeatherEntry.buildWeatherLocationWithDate(
        mLocation, mDateStr);
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
    if (data != null && data.moveToFirst()) {
      int weatherId = data.getInt(data.getColumnIndex(WeatherEntry.COLUMN_WEATHER_ID));
      mIconView.setImageResource(Utility.getArtResourceForWeatherCondition(weatherId));
      String date = data.getString(data.getColumnIndex(WeatherEntry.COLUMN_DATETEXT));
      String friendlyDateText = Utility.getDayName(getActivity(), date);
      String dateText = Utility.getFormattedMonthDay(getActivity(), date);
      mFriendlyDateView.setText(friendlyDateText);
      mDateView.setText(dateText);

      String description = data.getString(data.getColumnIndex(
          WeatherEntry.COLUMN_SHORT_DESC));
      mDescriptionView.setText(description);

      mIconView.setContentDescription(description);

      boolean isMetric = Utility.isMetric(getActivity());

      double high = data.getDouble(data.getColumnIndex(WeatherEntry.COLUMN_MAX_TEMP));
      String highString = Utility.formatTemperature(getActivity(), high);
      mHighTempView.setText(highString);
      double low = data.getDouble(data.getColumnIndex(WeatherEntry.COLUMN_MIN_TEMP));
      String lowString = Utility.formatTemperature(getActivity(), low);
      mLowTempView.setText(lowString);

      float humidity = data.getFloat(data.getColumnIndex(WeatherEntry.COLUMN_HUMIDITY));
      mHumidityView.setText(getActivity().getString(R.string.format_humidity, humidity));

      float windSpeedStr = data.getFloat(data.getColumnIndex(WeatherEntry.COLUMN_WIND_SPEED));
      float windDirStr = data.getFloat(data.getColumnIndex(WeatherEntry.COLUMN_DEGREES));
      mWindView.setText(Utility.getFormattedWind(getActivity(), windSpeedStr, windDirStr));

      float pressure = data.getFloat(data.getColumnIndex(WeatherEntry.COLUMN_PRESSURE));
      mPressureView.setText(getActivity().getString(R.string.format_pressure, pressure));
      mForecast = String.format("%s - %s - %s/%s", dateText, description, high, low);
      if (mShareActionProvider != null) {
        mShareActionProvider.setShareIntent(createShareForecastIntent());
      }
    }
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) { }
}