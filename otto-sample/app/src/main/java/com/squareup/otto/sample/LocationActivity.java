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
import android.support.v4.app.FragmentActivity;
import android.view.View;
import com.squareup.otto.Produce;

import java.util.Random;

import static android.view.View.OnClickListener;

public class LocationActivity extends FragmentActivity {
  public static final float DEFAULT_LAT = 40.440866f;
  public static final float DEFAULT_LON = -79.994085f;
  private static final float OFFSET = 0.1f;
  private static final Random RANDOM = new Random();

  private static float lastLatitude = DEFAULT_LAT;
  private static float lastLongitude = DEFAULT_LON;

  @Override protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.location_history);

    findViewById(R.id.clear_location).setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        // Tell everyone to clear their location history.
        BusProvider.getInstance().post(new LocationClearEvent());

        // Post new location event for the default location.
        lastLatitude = DEFAULT_LAT;
        lastLongitude = DEFAULT_LON;
        BusProvider.getInstance().post(produceLocationEvent());
      }
    });

    findViewById(R.id.move_location).setOnClickListener(new OnClickListener() {
      @Override public void onClick(View v) {
        lastLatitude += (RANDOM.nextFloat() * OFFSET * 2) - OFFSET;
        lastLongitude += (RANDOM.nextFloat() * OFFSET * 2) - OFFSET;
        BusProvider.getInstance().post(produceLocationEvent());
      }
    });
  }

  @Override protected void onResume() {
    super.onResume();

    // Register ourselves so that we can provide the initial value.
    BusProvider.getInstance().register(this);
  }

  @Override protected void onPause() {
    super.onPause();

    // Always unregister when an object no longer should be on the bus.
    BusProvider.getInstance().unregister(this);
  }

  @Produce public LocationChangedEvent produceLocationEvent() {
    // Provide an initial value for location based on the last known position.
    return new LocationChangedEvent(lastLatitude, lastLongitude);
  }
}
