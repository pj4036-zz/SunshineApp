package com.example.android.sunshine.app;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;

/**
 * Created by prateekjain on 9/9/15
 */
public class DetailActivity extends ActionBarActivity {

  public static final String DATE_KEY = "forecast_date";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_detail);

    if (savedInstanceState == null) {
      String date = getIntent().getStringExtra(DATE_KEY);
      Bundle arguments = new Bundle();
      arguments.putString(DetailActivity.DATE_KEY, date);
      DetailFragment fragment = new DetailFragment();
      fragment.setArguments(arguments);

      getSupportFragmentManager().beginTransaction()
          .add(R.id.weather_detail_container, fragment)
          .commit();
    }
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    getMenuInflater().inflate(R.menu.detail, menu);
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
}