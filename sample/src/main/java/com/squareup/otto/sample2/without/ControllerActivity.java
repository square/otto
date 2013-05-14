package com.squareup.otto.sample2.without;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

public class ControllerActivity extends FragmentActivity implements FragmentA.Listener,
    FragmentB.Listener {

  private FragmentA fragmentA;
  private FragmentB fragmentB;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    fragmentA = new FragmentA();
    fragmentA.setListener(this);
    fragmentB = new FragmentB();
    fragmentB.setListener(this);
    getSupportFragmentManager().beginTransaction()
        .add(android.R.id.content, fragmentA)
        .commit();
  }

  @Override public void onClickedB() {
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, fragmentB)
        .commit();
  }

  @Override public void onClickedA() {
    getSupportFragmentManager().beginTransaction()
        .replace(android.R.id.content, fragmentA)
        .commit();
  }
}
