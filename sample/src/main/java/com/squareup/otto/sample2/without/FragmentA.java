package com.squareup.otto.sample2.without;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.squareup.otto.sample.R;

public class FragmentA extends Fragment {
  private Listener listener;

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_a, container, false);
    view.findViewById(R.id.goto_b).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (listener != null) {
          listener.onClickedB();
        }
      }
    });
    return view;
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public interface Listener {
    void onClickedB();
  }
}
