/*
 * Copyright (C) 2012 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.squareup.otto.sample;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.View;
import android.widget.ArrayAdapter;
import com.squareup.otto.Callback;
import com.squareup.otto.Subscribe;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

/** Maintain a scrollable history of location events. */
public class LocationHistoryFragment extends ListFragment {
  private final List<String> locationEvents = new ArrayList<String>();
  private ArrayAdapter<String> adapter;
  private Callback callback;

  @Override public void onResume() {
    super.onResume();
    BusProvider.getInstance().register(this);
    // Example of explicit registration of Callbacks
    callback = new Callback() {
      @Override
      public void call(Object event) throws InvocationTargetException {
        locationEvents.add(0, event.toString());
        if (adapter != null) {
          adapter.notifyDataSetChanged();
        }
      }
    };
    BusProvider.getInstance().register(LocationChangedEvent.class, callback);
  }

  @Override public void onPause() {
    super.onPause();
    BusProvider.getInstance().unregister(this);
    // Example of explicit un-registration of Callbacks
    BusProvider.getInstance().unregister(LocationHistoryFragment.class, callback);
  }

  @Override public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, locationEvents);
    setListAdapter(adapter);
  }

  @Subscribe public void onLocationCleared(LocationClearEvent event) {
    locationEvents.clear();
    if (adapter != null) {
      adapter.notifyDataSetChanged();
    }
  }
}
