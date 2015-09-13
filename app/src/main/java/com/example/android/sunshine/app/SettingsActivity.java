package com.example.android.sunshine.app;

import android.annotation.TargetApi;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.example.android.sunshine.app.data.WeatherContract;
import com.example.android.sunshine.app.sync.SunshineSyncAdapter;

public class SettingsActivity extends PreferenceActivity
    implements Preference.OnPreferenceChangeListener {
  boolean mBindingPreference;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.pref_general);
    bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_location_key)));
    bindPreferenceSummaryToValue(findPreference(getString(R.string.pref_units_key)));
  }

  private void bindPreferenceSummaryToValue(Preference preference) {
    mBindingPreference = true;
    preference.setOnPreferenceChangeListener(this);
    onPreferenceChange(preference,
        PreferenceManager
            .getDefaultSharedPreferences(preference.getContext())
            .getString(preference.getKey(), ""));

    mBindingPreference = false;
  }

  @Override
  public boolean onPreferenceChange(Preference preference, Object value) {
    String stringValue = value.toString();
    if ( !mBindingPreference ) {
      if (preference.getKey().equals(getString(R.string.pref_location_key))) {
        SunshineSyncAdapter.syncImmediately(this);
      } else {
        getContentResolver().notifyChange(WeatherContract.WeatherEntry.CONTENT_URI, null);
      }
    }

    if (preference instanceof ListPreference) {
      ListPreference listPreference = (ListPreference) preference;
      int prefIndex = listPreference.findIndexOfValue(stringValue);
      if (prefIndex >= 0) {
        preference.setSummary(listPreference.getEntries()[prefIndex]);
      }
    } else {
      preference.setSummary(stringValue);
    }
    return true;
  }

  @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
  @Override
  public Intent getParentActivityIntent() {
    return super.getParentActivityIntent().addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
  }
}
