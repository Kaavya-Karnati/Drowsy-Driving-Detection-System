package com.example.drowsydrivingdetection.voice;

public class AlertThresholdCounter {
    private static final String TAG = "AlertThresholdCounter";

    //how many alerts need to be triggered to send this specific alert
    //can be a combination of visual + auditory >= 3
    private static final int VOICE_TRIGGER_THRESHOLD = 3;

    //cooldown time between each voice feedback alert sent
    private static final long SESSION_COOLDOWN_MS = 60_000;

    private int audioAlertCount;
    private int visualAlertCount;

    //whether a voice session is already in progress
    private boolean sessionActive;

    //timestamp of the last completed session
    private long lastSessionEndTime;

    public AlertThresholdCounter() {
        audioAlertCount = 0;
        visualAlertCount = 0;
        sessionActive = false;
        lastSessionEndTime = 0;
    }

    public void onAudioAlertFired() {
        audioAlertCount++;
    }

    public void onVisualAlertFired() {
        visualAlertCount++;
    }

    //return true if alert count threshold is met, no current active session and
    //cooldown period has ended
    public boolean shouldTriggerVoiceFeedback() {
        if (sessionActive) return false;
        if (getCombinedCount() < VOICE_TRIGGER_THRESHOLD) return false;

        long now = System.currentTimeMillis();
        return (now - lastSessionEndTime) >= SESSION_COOLDOWN_MS;
    }

    //call this the moment a voice session starts to prevent double-triggering
    public void onSessionStarted() {
        sessionActive = true;
    }

    //called after every session ends, no matter the outcome
    //resets the counts and logs the time of the last time the alert was triggered
    public void resetAfterSession() {
        audioAlertCount = 0;
        visualAlertCount = 0;
        sessionActive = false;
        lastSessionEndTime = System.currentTimeMillis();
    }

    public int getAudioAlertCount() {
        return audioAlertCount;
    }

    public int getVisualAlertCount() {
        return visualAlertCount;
    }

    public int getCombinedCount() {
        return audioAlertCount + visualAlertCount;
    }

    public boolean isSessionActive() {
        return sessionActive;
    }

}
