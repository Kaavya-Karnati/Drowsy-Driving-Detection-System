package com.example.drowsydrivingdetection;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.util.Log;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;


public class DrowsinessTracker {
    private static final String TAG = "DrowsinessTracker";

    //configuration for time and alert threshold logic
    private static final int WINDOW_SIZE = 90;  //3 seconds at 30 FPS
    private static final long EYES_CLOSED_THRESHOLD_MS = 2000;  //3 seconds for audio alert
    private static final int YAWN_COUNT_THRESHOLD = 30;  //3 yawns for visual alert
    //several yawns detected (since real time yawning lasts for a few second, each frame 
    //counts as a yawn, so for now replace with 14 (14 detections roughly amount to 3 yawns))
    private static final long YAWN_WINDOW_MS = 60000;  //Count yawns in last 60 seconds

    //Sliding window of recent detections;
    //using a double sided queue as the data structure that allows insertion, removal and retrieval
    //from both ends of the queue. It also grows dynamically if needed, so the best data structure
    //to use to act as a moving window going through all the detections from model inference
    private Deque<CleanDetectionResult> frameHistory;

    //yawn tracking
    private List<Long> yawnTimestamps;

    //state tracking
    private long eyesClosedStartTime;
    private boolean currentlyEyesClosed;
    private boolean audioAlertActive;

    // need sharedPreferences to save to the alert dashboard
    private SharedPreferences sharedPreferences;

    public DrowsinessTracker(SharedPreferences sharedPreferences) {
        this.frameHistory = new ArrayDeque<>(WINDOW_SIZE);
        this.yawnTimestamps = new ArrayList<>();
        this.eyesClosedStartTime = 0;
        this.currentlyEyesClosed = false;
        this.audioAlertActive = false;
        this.sharedPreferences = sharedPreferences;
    }

    public void addFrame(CleanDetectionResult result) {
        //add to window
        frameHistory.addLast(result);

        //keep window size limited
        if (frameHistory.size() > WINDOW_SIZE) {
            frameHistory.removeFirst();
        }

        //track eyes closed state
        updateEyesClosedState(result);

        //track yawns
        if (result.yawnCount > 0 && getRecentYawnCount() < YAWN_COUNT_THRESHOLD) {
            yawnTimestamps.add(result.timestamp);
            cleanOldYawns(result.timestamp);
            Log.d(TAG, "Yawn detected! Total in window: " + getRecentYawnCount());
        }
    }

    private void updateEyesClosedState(CleanDetectionResult result) {
        boolean eyesClosedNow = result.eyesClosedCount > 0;

        if (eyesClosedNow && !currentlyEyesClosed) {
            //eyes just closed
            eyesClosedStartTime = result.timestamp;
            currentlyEyesClosed = true;
            Log.d(TAG, "Eyes closed: starting timer");

        } else if (!eyesClosedNow && currentlyEyesClosed) {
            //eyes just opened
            long duration = result.timestamp - eyesClosedStartTime;
            Log.d(TAG, "Eyes opened after " + duration + "ms");
            currentlyEyesClosed = false;
            eyesClosedStartTime = 0;
            audioAlertActive = false;  // Dismiss alert when eyes open

        } else if (eyesClosedNow && currentlyEyesClosed) {
            //eyes still closed so, check duration
            long duration = result.timestamp - eyesClosedStartTime;
            if (duration >= EYES_CLOSED_THRESHOLD_MS && !audioAlertActive) {
                Log.d(TAG, "Eyes closed for " + duration + "ms --- TRIGGER AUDIO ALERT ---");
                audioAlertActive = true;
            }
        }
    }

    //remove yawns older than the window (60 seconds)
    private void cleanOldYawns(long currentTime) {
        yawnTimestamps.removeIf(timestamp ->
                (currentTime - timestamp) > YAWN_WINDOW_MS
        );
    }

    //get number of yawns in recent window
    public int getRecentYawnCount() {
        return yawnTimestamps.size();
    }

    //check if audio alert should be triggered
    //trigger if eyes closed for >= 3 seconds
    public boolean shouldTriggerAudioAlert() {
        return audioAlertActive && currentlyEyesClosed;
    }

    //check if visual alert should be triggered
    //trigger if 3 or more yawns detected
    public boolean shouldTriggerVisualAlert() {
        return getRecentYawnCount() >= YAWN_COUNT_THRESHOLD;
    }

    //get current drowsiness level for UI display
    @SuppressLint("DefaultLocale")
    public String getDrowsinessLevel() {
        if (shouldTriggerAudioAlert()) {
            return "CRITICAL: Eyes Closed!";
        } else if (shouldTriggerVisualAlert()) {
            return "WARNING: Fatigue Detected";
        } else if (currentlyEyesClosed) {
            long duration = System.currentTimeMillis() - eyesClosedStartTime;
            return String.format("Eyes Closed (%.1fs)", duration / 1000.0);
        } else if (getRecentYawnCount() > 0) {
            return String.format("Yawns: %d/%d", getRecentYawnCount(), YAWN_COUNT_THRESHOLD);
        } else {
            return "Alert";
        }
    }

    public void reset() {
        frameHistory.clear();
        yawnTimestamps.clear();
        eyesClosedStartTime = 0;
        currentlyEyesClosed = false;
        audioAlertActive = false;
    }

    public void resetYawns() {
        yawnTimestamps.clear();
    }

    public void dismissAudioAlert() {
        audioAlertActive = false;
    }

    public void saveAudioAlertCount() {
        int currentAlerts = sharedPreferences.getInt("audio_alert", 0);

        Log.d(TAG, "Saved audio alert, current count: " + currentAlerts);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("audio_alert", currentAlerts + 1);
        editor.apply();
    }

    public void saveVisualAlertCount() {
        int currentAlerts = sharedPreferences.getInt("visual_alert", 0);

        Log.d(TAG, "Saved visual alert, current count: " + currentAlerts);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt("visual_alert", currentAlerts + 1);
        editor.apply();
    }
}