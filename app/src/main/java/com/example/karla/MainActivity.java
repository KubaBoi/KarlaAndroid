package com.example.karla;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.StrictMode;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * Class represents main class of project. It creates main screen.
 */
public class MainActivity extends Activity {
    /**
     * Media player for main screen music
     */
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;

    // Requesting permission to RECORD_AUDIO
    private boolean permissionToRecordAccepted = false;
    private String [] permissions = {Manifest.permission.RECORD_AUDIO};

    /* constants */
    private static final String LOG_TAG = "KARLA_MAIN";
    private static final int POLL_INTERVAL = 10;


    /** config state **/
    private double mThreshold = 1.0;

    private PowerManager.WakeLock mWakeLock;

    private Handler mHandler = new Handler();
    private CommunicationManager comM;

    /* data source */
    private MediaPlayer player = null;
    private SoundMeter mSensor;
    private String inputFile;
    private String outputFile;

    private boolean waiting = false;
    private boolean recording = false;
    private int silenceTime = 0;
    private int maxSilenceTime = 200;
    private int hearingTime = 0;
    private int maxHearingTime = 100;


    /**
     * Initiating all buttons for navigating through project.
     */
    @SuppressLint("InvalidWakeLockTag")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this, permissions, REQUEST_RECORD_AUDIO_PERMISSION);

        inputFile = getExternalCacheDir().getAbsolutePath();
        inputFile += "/temp.mp3";

        outputFile = getExternalCacheDir().getAbsolutePath();
        outputFile += "/tempOut.mp3";

        player = new MediaPlayer();

        mSensor = new SoundMeter();
        mSensor.start(inputFile);

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), "TAG");

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        comM = new CommunicationManager();
        comM.start(inputFile, outputFile);

        JSONObject obj = new JSONObject();
        try {
            obj.put("TEXT", "Hello Kuba");
        } catch (JSONException e) {
            e.printStackTrace();
        }

        if (comM.getFromJson(obj)) {
            speak();
        }

        mPollTask.run();
    }

    private Runnable mPollTask = new Runnable() {
        @Override
        public void run() {
            if (waiting) {
                mHandler.postDelayed(mPollTask, POLL_INTERVAL*5);
                return;
            }

            double amp = mSensor.getAmplitudeEMA();
            if ((amp > mThreshold && !recording)) {
                Log.e(LOG_TAG, Double.toString(amp));
                mWakeLock.acquire();
                recording = true;
            }
            else if (!recording) { //aby ta .wav nebyla moc velka
                hearingTime++;
                if (hearingTime >= maxHearingTime) {
                    Log.e(LOG_TAG, "REMOVING OLD FILE");
                    mSensor.stop();
                    mSensor.start(inputFile);
                    hearingTime = 0;
                }
            }
            else if (recording) {
                if (amp < mThreshold) silenceTime++;
                else silenceTime = 0;

                if (silenceTime >= maxSilenceTime) {
                    Log.e(LOG_TAG, "Saving...");
                    silenceTime = 0;
                    mSensor.stop(); // stopping recording

                    recording = false;
                    waiting = true;

                    if (comM.getFromSound()) { // communicating with backend
                        speak(); // speaking
                    } else {
                        mSensor.start(inputFile);
                    }
                }
            }

            mHandler.postDelayed(mPollTask, POLL_INTERVAL);
        }
    };

    private void speak() {
        try {
            player = new MediaPlayer();
            player.setDataSource(outputFile);
            player.prepare();
            player.start();
            player.setOnCompletionListener(mp -> {
                Log.e(LOG_TAG, "STOP PLAYING");

                mp.release();
                mp = null;

                mSensor.start(inputFile); // starting recording again
                waiting = false;
            });
            player.setLooping(false);
            Log.e(LOG_TAG, "START PLAYING");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_RECORD_AUDIO_PERMISSION:
                permissionToRecordAccepted  = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted ) finish();

    }


}
