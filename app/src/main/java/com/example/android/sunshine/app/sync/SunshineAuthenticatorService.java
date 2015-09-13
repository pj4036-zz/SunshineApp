package com.example.android.sunshine.app.sync;

/**
 * Created by prateekjain on 8/14/15.
 */

    import android.app.Service;
    import android.content.Intent;
    import android.os.IBinder;

public class SunshineAuthenticatorService extends Service {
  private SunshineAuthenticator mAuthenticator;

  @Override
  public void onCreate() {
    mAuthenticator = new SunshineAuthenticator(this);
  }
  @Override
  public IBinder onBind(Intent intent) {
    return mAuthenticator.getIBinder();
  }
}