/*
 * Copyright (C) 2024 Neoteric OS
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package co.aospa.glyph.Services;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.List;

import co.aospa.glyph.Constants.Constants;
import co.aospa.glyph.Manager.AnimationManager;
import co.aospa.glyph.Manager.StatusManager;
import co.aospa.glyph.Utils.ResourceUtils;

public class AutoBrightnessService extends Service {

    private static final String TAG = "GlyphAutoBrightnessService";
    private static final boolean DEBUG = true;

    private SensorManager mSensorManager;
    private Sensor mLightSensor;
    private static int sensorType;
    // Lazy initialized to avoid crash before Constants.CONTEXT is set
    private static int[] AutoBrightnessLux = null;
    private static int[] BrightnessValues = null;

    private static int[] getAutoBrightnessLux() {
        if (AutoBrightnessLux == null) {
            AutoBrightnessLux = ResourceUtils.getIntArray("glyph_auto_brightness_levels");
        }
        return AutoBrightnessLux;
    }

    private static int[] getBrightnessValues() {
        if (BrightnessValues == null) {
            BrightnessValues = Constants.getBrightnessLevels();
        }
        return BrightnessValues;
    }

    @Override
    public void onCreate() {
        if (DEBUG) Log.d(TAG, "Creating service");

        // Initialize context before anything else
        if (Constants.CONTEXT == null) {
            Constants.CONTEXT = getApplicationContext();
        }

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        // Get light sensor type
        String sensorName = ResourceUtils.getString("glyph_light_sensor");
        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        for (Sensor sensor : sensors) {
            if (sensorName.equals(sensor.getStringType())) {
                sensorType = sensor.getType();
                break;
            }
        }

        mLightSensor = mSensorManager.getDefaultSensor(sensorType);
        mSensorManager.registerListener(mSensorEventListener,
            mLightSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (DEBUG) Log.d(TAG, "Starting service");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (DEBUG) Log.d(TAG, "Destroying service");
        mSensorManager.unregisterListener(mSensorEventListener);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final SensorEventListener mSensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            int lux = (int) event.values[0];
            int lux_index = 0;
            int brightnessValue;

            for (int i = 1; i < getAutoBrightnessLux().length; i++) {
                if (lux < getAutoBrightnessLux()[i]) {
                    break;
                } else if (lux >= getAutoBrightnessLux()[i]) {
                    lux_index = i;
                }
            }

            brightnessValue = getBrightnessValues()[lux_index];

            if (brightnessValue != Constants.getBrightness()) {
                if (DEBUG) {
                    int led_lux = getAutoBrightnessLux()[lux_index];
                    Log.d(TAG, "Brightness changed: " + "RealLux: " + lux + 
                    " | BrightnessLux: " + led_lux + " | BrightnessValue: " + brightnessValue);
                }
                Constants.setBrightness(brightnessValue);
                if (StatusManager.isEssentialLedActive())
                    AnimationManager.playEssential();
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };
}
