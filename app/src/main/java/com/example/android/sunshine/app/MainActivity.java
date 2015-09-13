package com.example.android.sunshine.app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.example.android.sunshine.app.sync.SunshineSyncAdapter;


public class MainActivity extends ActionBarActivity implements ForecastFragment.Callback {

  private final String LOG_TAG = MainActivity.class.getSimpleName();

  private boolean mTwoPane;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    if (findViewById(R.id.weather_detail_container) != null) {
      mTwoPane = true;
      if (savedInstanceState == null) {
        getSupportFragmentManager().beginTransaction()
            .replace(R.id.weather_detail_container, new DetailFragment())
            .commit();
      }
    } else {
      mTwoPane = false;
    }

    ForecastFragment forecastFragment =  ((ForecastFragment)getSupportFragmentManager()
        .findFragmentById(R.id.fragment_forecast));
    forecastFragment.setUseTodayLayout(!mTwoPane);

    SunshineSyncAdapter.initializeSyncAdapter(this);

  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.main, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_settings) {
      startActivity(new Intent(this, SettingsActivity.class));
      return true;
    }
    return super.onOptionsItemSelected(item);
  }


  @Override
  public void onItemSelected(String date) {
    if (mTwoPane) {
      Bundle args = new Bundle();
      args.putString(DetailActivity.DATE_KEY, date);

      DetailFragment fragment = new DetailFragment();
      fragment.setArguments(args);

      getSupportFragmentManager().beginTransaction()
          .replace(R.id.weather_detail_container, fragment)
          .commit();
    } else {
      Intent intent = new Intent(this, DetailActivity.class)
          .putExtra(DetailActivity.DATE_KEY, date);
      startActivity(intent);
    }
  }
}
