package com.squareup.otto.sample2.with;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import com.squareup.otto.BusProvider;
import com.squareup.otto.Subscribe;

public class ControllerActivity extends FragmentActivity {

  private FragmentA fragmentA;
  private FragmentB fragmentB;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fragmentA = new FragmentA();
    fragmentB = new FragmentB();
    getSupportFragmentManager().beginTransaction()
        .add(android.R.id.content, fragmentA)
        .commit();
  }

  @Override protected void onResume() {
    super.onResume();
    BusProvider.getInstance().register(this);
  }

  @Override protected void onPause() {
    super.onPause();
    BusProvider.getInstance().unregister(this);
  }

  @Subscribe public void onClickedB(StartBEvent event) {
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, fragmentB)
        .commit();
  }

  @Subscribe public void onClickedA(StartAEvent event) {
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, fragmentA)
        .commit();
  }
}
