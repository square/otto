package com.squareup.otto.sample2.with;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.squareup.otto.BusProvider;
import com.squareup.otto.sample.R;

public class FragmentB extends Fragment {
  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_b, container, false);
    view.findViewById(R.id.goto_a).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        BusProvider.getInstance().post(new StartAEvent());
      }
    });
    return view;
  }
}
