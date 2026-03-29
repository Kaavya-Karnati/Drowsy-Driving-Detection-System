package com.example.drowsydrivingdetection.voice;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.concurrent.atomic.AtomicBoolean;

/*
  Orchestrates voice check-in after repeated drowsiness alerts.
  Integrates with AlertThresholdCounter, TTSHelper, SpeechRecognitionHelper,
  and ResponseAnalyzer
*/
public final class VoiceFeedbackManager {

    public static final String TAG = "VoiceFeedback";
    public static final String LOG_SMS_TAG = "EmergencySMS";

    public static final String PREF_DRIVER_VOICE_OK = "driver_voice_reported_alert_ok";
    public static final String PREF_DRIVER_VOICE_OK_TIME = "driver_voice_reported_alert_ok_time";

    //time period an user can respond after the listen phase starts
    private static final long LISTEN_WINDOW_MS = 14_000;

    //pause after TTS (question prompt) so the recognizer mic is not competing with other sounds
    private static final long POST_TTS_BEFORE_LISTEN_MS = 1_800;
    private static final int ESCALATION_SECONDS = 5; //timer before mock 911
    // (we currently just use Ahmed's text feature) call is made

    public interface Host {
        AppCompatActivity getActivity();
        //needed to pass to SpeechRecognitionHelper, which requires an Activity context

        SharedPreferences getPrefs();
        //returns the same SharedPreferences the rest of the app uses for the dashboard

        void stopAlarmSoundOnly();
        //called at the start of a voice session to mute the drowsiness alarm so it doesn't compete with TTS audio
        //without resetting the drowsiness tracker state

        boolean hasRecordAudioPermission();
        //checked before starting the microphone, since recording requires runtime permission

        void requestRecordAudioPermission(int requestCode);
        //triggers the system permission dialog if the permission isn't yet granted
    }

    private final Host host;
    private final AlertThresholdCounter alertThresholdCounter;
    private final TTSHelper ttsHelper;

    @Nullable
    private SpeechRecognitionHelper speechHelper;
    //created on first use but not on construction
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    //main thread scheduler
    //every timed operation in this class flows through this

    @Nullable
    private View overlay;
    @Nullable
    private TextView statusText;
    @Nullable
    private Button btnYes;
    @Nullable
    private Button btnGetHelp;
    @Nullable
    private TextView escalationCountdown;
    @Nullable
    private Button btnCancelEmergency;
    //all six UI references are @Nullable
    //They start as null and are only populated if the caller invokes bindUi()

    private final AtomicBoolean listenPhaseFinished = new AtomicBoolean(true);
    /* The listen phase can end in three ways:
        the speech recognizer returns a result,
        the 14-second cutoff timer fires, or the user taps a button.
        All three can happen at nearly the same time. AtomicBoolean ensures that only the first one
        to call compareAndSet(false, true) is considered
    */
    private final Runnable listenCutoffRunnable = () -> {
        cancelSpeechRecognitionSafely();
        finishListenPhase(null, -1f);
    };
    //runnable runs after LISTEN_WINDOW_MS (14 seconds) if no response arrives
    //it cancels the speech recognizer without triggering its callback, then calls
    // finishListenPhase() with null transcript and -1 confidence
    private final Runnable beginListenAfterTtsRunnable = this::beginListenWindow;
    //stored reference to beginListenWindow so it can be both scheduled with postDelayed()
    //and cancelled with removeCallbacks() using the same object reference
    //similar to how SpeechRecognizerHelper works

    @Nullable
    private Runnable escalationTicker;
    private int escalationRemaining = ESCALATION_SECONDS;
    //tracks the time left until mock 911 call is left and the seconds

    public static final int REQUEST_RECORD_AUDIO = 4101;
    //The integer code passed to requestRecordAudioPermission() and matched in onRequestPermissionsResult()
    //4101 is an arbitrary number

    public VoiceFeedbackManager(Host host) {
        this.host = host;
        this.alertThresholdCounter = new AlertThresholdCounter();
        AppCompatActivity act = host.getActivity();
        this.ttsHelper = new TTSHelper(act);
    }
    /*
    Creates the two always-needed collaborators (Host and AlertThresholdCounter)
    AlertThresholdCounter starts with zeroed counts
    TTSHelper starts initializing asynchronously in the background immediately
    (by the time the first voice session is triggered, TTS will likely already be ready)
    SpeechRecognitionHelper is intentionally NOT created here and it waits until it's actually needed.
     */

    public void bindUi(
            @Nullable View overlay,
            @Nullable TextView statusText,
            @Nullable Button btnYes,
            @Nullable Button btnGetHelp,
            @Nullable TextView escalationCountdown,
            @Nullable Button btnCancelEmergency
    ) {
        this.overlay = overlay;
        this.statusText = statusText;
        this.btnYes = btnYes;
        this.btnGetHelp = btnGetHelp;
        this.escalationCountdown = escalationCountdown;
        this.btnCancelEmergency = btnCancelEmergency;

        if (btnYes != null) {
            btnYes.setOnClickListener(v -> onTapYes());
        }
        if (btnGetHelp != null) {
            btnGetHelp.setOnClickListener(v -> onTapGetHelp());
        }
        if (btnCancelEmergency != null) {
            btnCancelEmergency.setOnClickListener(v -> cancelEscalationCountdown());
        }
    }

    //true after a session hits the threshold or manual start
    public boolean isVoiceSessionActive() {
        return alertThresholdCounter.isSessionActive();
    }

    public void notifyDrowsinessAlertFired(boolean isAudioAlert) {
        if (alertThresholdCounter.isSessionActive()) {
            return;
        }

        if (isAudioAlert) {
            alertThresholdCounter.onAudioAlertFired();
        } else {
            alertThresholdCounter.onVisualAlertFired();
        }

        Log.d(TAG, "Alert tally: audio=" + alertThresholdCounter.getAudioAlertCount()
                + " visual=" + alertThresholdCounter.getVisualAlertCount()
                + " combined=" + alertThresholdCounter.getCombinedCount());

        if (!alertThresholdCounter.shouldTriggerVoiceFeedback()) {
            return;
        }

        //claim session on this thread before posting UI so a second camera callback cannot
        //enqueue another start before the first run begins. this was complicated for me, but
        //this relates to the threading and callbacks
        alertThresholdCounter.onSessionStarted();
        mainHandler.post(this::startVoiceSessionOnMainThread);
    }


    //test button which triggers without the alert threshold to meet requirements, faster
    //testing and development time
    public void startManualTestSession() {
        if (alertThresholdCounter.isSessionActive()) {
            Log.w(TAG, "Voice session already active: ignoring manual start");
            return;
        }
        Log.i(TAG, "Manual test: starting voice session");
        alertThresholdCounter.onSessionStarted();
        mainHandler.post(this::startVoiceSessionOnMainThread);
    }

    /*
    Called by your Activity's onRequestPermissionsResult() to forward the permission result here
    Two guards: ignore results for other permission request codes
    and ignore results if no session is active
    (the user may have been prompted for permission from a previous session that already ended)

    If denied, it logs a warning but still calls beginListenWindow():
     which will fall through to buttons-only mode
     */
    public void onRequestPermissionsResult(int requestCode, boolean granted) {
        if (requestCode != REQUEST_RECORD_AUDIO) {
            return;
        }
        if (!alertThresholdCounter.isSessionActive()) {
            return;
        }
        if (!granted) {
            Log.w(TAG, "RECORD_AUDIO denied -- on-screen buttons only");
        }
        beginListenWindow();
    }

    public void destroy() {
        mainHandler.removeCallbacks(listenCutoffRunnable);
        mainHandler.removeCallbacks(beginListenAfterTtsRunnable);
        //clearing callbacks

        clearEscalationTicker();
        if (speechHelper != null) {
            try {
                speechHelper.destroy();
            } catch (RuntimeException e) {
                Log.w(TAG, "speechHelper.destroy() failed", e);
            }
            speechHelper = null;
        }
        ttsHelper.shutdown();

        //destroys down both audio helpers
        //speechHelper.destroy() is wrapped in a try-catch because Android's SpeechRecognizer can throw on destroy()
        //if the service was already disconnected.
        //ttsHelper.shutdown() is not wrapped and TTSHelper.shutdown() is implemented to be safe to call at any time
    }

    /*
        Returns the existing instance if it exists.
        Otherwise attempts to create one, catching Throwable (not just Exception) because
        SpeechRecognizer initialization can throw Android-internal errors.

        If creation fails, returns null and the caller falls back to buttons-only mode
     */
    @Nullable
    private SpeechRecognitionHelper getOrCreateSpeechHelper() {
        if (speechHelper != null) {
            return speechHelper;
        }
        AppCompatActivity act = host.getActivity();
        if (act == null) {
            Log.w(TAG, "Cannot create SpeechRecognitionHelper: activity is null");
            return null;
        }
        try {
            speechHelper = new SpeechRecognitionHelper(act);
            return speechHelper;

        } catch (Throwable t) {
            Log.e(TAG, "SpeechRecognitionHelper init failed", t);
            speechHelper = null;
            return null;
        }
    }

    private void cancelSpeechRecognitionSafely() {
        SpeechRecognitionHelper helper = speechHelper;
        if (helper == null) {
            return;
        }
        try {
            helper.cancel();
        } catch (RuntimeException e) {
            Log.w(TAG, "speechHelper.cancel() failed", e);
        }
    }

    private void startVoiceSessionOnMainThread() {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            mainHandler.post(this::startVoiceSessionOnMainThread);
            return;
        }
        //self-correcting thread guard. If this method is somehow called from a background thread,
        //it re-posts itself to the main thread and returns.
        // Since all view updates happen here, running on the wrong thread would cause crashes
        host.stopAlarmSoundOnly();

        showOverlay(true);
        setStatus("Voice check: please listen…");
        showEscalationUi(false);

        listenPhaseFinished.set(true);

        ttsHelper.speakDriverPrompt(new TTSHelper.OnSpeechDoneListener() {
            @Override
            public void onDone() {
                mainHandler.post(() -> maybeBeginListenAfterTts());
            }

            @Override
            public void onError(String message) {
                Log.w(TAG, "TTS error: " + message + "-- continuing with buttons");
                mainHandler.post(() -> maybeBeginListenAfterTts());
            }
        });
    }

    private void maybeBeginListenAfterTts() {
        if (!alertThresholdCounter.isSessionActive()) {
            return;
        }
        setStatus(TTSHelper.DRIVER_PROMPT + "\n\nRespond aloud, or tap a button.\n(Listening… you have a few seconds.)");

        if (host.hasRecordAudioPermission()) {
            mainHandler.removeCallbacks(beginListenAfterTtsRunnable);
            mainHandler.postDelayed(beginListenAfterTtsRunnable, POST_TTS_BEFORE_LISTEN_MS);
        } else {
            setStatus(TTSHelper.DRIVER_PROMPT
                    + "\n\nRespond aloud, or tap a button.\n(Microphone permission needed — requesting.)");
            host.requestRecordAudioPermission(REQUEST_RECORD_AUDIO);
        }

        //if the app has mic permission, schedule beginListenWindow()
        //if not, update the status text to tell the driver what's happening and request the permission
    }

    private void beginListenWindow() {
        if (!alertThresholdCounter.isSessionActive()) {
            return;
        }
        listenPhaseFinished.set(false);

        mainHandler.removeCallbacks(listenCutoffRunnable);
        mainHandler.postDelayed(listenCutoffRunnable, LISTEN_WINDOW_MS);
        //arms the 14-second cutoff.
        //any previously scheduled cutoff is cancelled first to prevent double-running

        if (!host.hasRecordAudioPermission()) {
            Log.w(TAG, "No RECORD_AUDIO: on-screen buttons only for " + LISTEN_WINDOW_MS + " ms");
            return;
        }

        SpeechRecognitionHelper stt = getOrCreateSpeechHelper();
        if (stt == null) {
            Log.w(TAG, "Speech recognition unavailable: on-screen buttons only for "
                    + LISTEN_WINDOW_MS + " ms");
            return;
        }

        try {
            stt.listen(new SpeechRecognitionHelper.RecognitionCallback() {
                @Override
                public void onResult(String transcript, float confidence) {
                    mainHandler.post(() -> finishListenPhase(transcript, confidence));
                }

                @Override
                public void onFailure(int errorCode, String errorDesc) {
                    Log.w(TAG, "Speech failed " + errorCode + ": " + errorDesc);
                    mainHandler.post(() -> finishListenPhase(null, -1f));
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "listen() threw", e);
            mainHandler.post(() -> finishListenPhase(null, -1f));
        }
    }

    private void finishListenPhase(@Nullable String transcript, float confidence) {
        if (!listenPhaseFinished.compareAndSet(false, true)) {
            return;
        }
        //compareAndSet(false, true) atomically checks if the value is false, and if so, sets it to true and returns true.
        //if it was already true (meaning another path already finished the phase)
        //it returns false and this method exits immediately.
        //This is what prevents both the STT result and the cutoff timer from both calling applyAnalysis()
        mainHandler.removeCallbacks(listenCutoffRunnable);

        ResponseAnalyzer.Result result = ResponseAnalyzer.classify(transcript, confidence);
        Log.d(TAG, "Listen phase: " + result);

        applyAnalysis(result);
    }

    private void onTapYes() {
        if (!alertThresholdCounter.isSessionActive()) {
            return;
        }
        if (!listenPhaseFinished.compareAndSet(false, true)) {
            return;
        }
        mainHandler.removeCallbacks(listenCutoffRunnable);
        cancelSpeechRecognitionSafely();
        markDriverAlertOk();
        endSessionAfterSuccess();
    }

    private void onTapGetHelp() {
        if (!alertThresholdCounter.isSessionActive()) {
            return;
        }
        if (!listenPhaseFinished.compareAndSet(false, true)) {
            return;
        }
        mainHandler.removeCallbacks(listenCutoffRunnable);
        cancelSpeechRecognitionSafely();
        startEscalationCountdown("You tapped Get Help.");
    }

    private void applyAnalysis(ResponseAnalyzer.Result result) {
        switch (result.classification) {
            case ALERT_OK:
                markDriverAlertOk();
                endSessionAfterSuccess();
                break;
            case ESCALATE:
            case NO_RESPONSE:
            case GIBBERISH:
            default:
                startEscalationCountdown(result.reason);
                break;
        }
        //ALERT_OK is the only path that ends happily
        //All three other outcomes: ESCALATE, NO_RESPONSE, and GIBBERISH lead to the escalation countdown
    }

    private void markDriverAlertOk() {
        SharedPreferences prefs = host.getPrefs();
        prefs.edit()
                .putBoolean(PREF_DRIVER_VOICE_OK, true)
                .putLong(PREF_DRIVER_VOICE_OK_TIME, System.currentTimeMillis())
                .apply();
        Log.i(TAG, "Driver marked alert/OK via voice or button");
    }

    private void endSessionAfterSuccess() {
        showOverlay(false);
        showEscalationUi(false);
        alertThresholdCounter.resetAfterSession();
        listenPhaseFinished.set(true);
    }
    //clean teardown for the happy path.
    //hides both the main overlay and the escalation UI, calls resetAfterSession() on AlertThresholdCounter
    // which zeros the counts and starts the 60-second cooldown, and resets the listen phase flag


    private void startEscalationCountdown(String reason) {
        setStatus("No safe confirmation.\n" + reason + "\n\nAssistance countdown seconds (tap Cancel to stop):");
        showEscalationUi(true);
        escalationRemaining = ESCALATION_SECONDS;
        clearEscalationTicker();
        escalationTicker = () -> {
            if (escalationCountdown != null) {
                escalationCountdown.setText(String.valueOf(escalationRemaining));
            }
            if (escalationRemaining <= 0) {
                mockSend911Sms();
                showOverlay(false);
                showEscalationUi(false);
                alertThresholdCounter.resetAfterSession();
                listenPhaseFinished.set(true);
                escalationTicker = null;
                return;
            }
            escalationRemaining--;
            mainHandler.postDelayed(escalationTicker, 1000);
        };
        mainHandler.post(escalationTicker);
        /*
        This is a self-rescheduling runnable. When it fires: it updates the countdown text,
        checks if the count has reached zero (and if so, fires the mock 911 call and tears everything down),
        and if not, decrements the counter and schedules itself to run again in 1 second.
         */
    }

    private void cancelEscalationCountdown() {
        clearEscalationTicker();
        Log.i(TAG, "User cancelled emergency assistance countdown");
        showOverlay(false);
        showEscalationUi(false);
        alertThresholdCounter.resetAfterSession();
        listenPhaseFinished.set(true);
    }

    private void clearEscalationTicker() {
        if (escalationTicker != null) {
            mainHandler.removeCallbacks(escalationTicker);
            escalationTicker = null;
        }
    }

    private void mockSend911Sms() {
        String msg = "MOCK 911 SMS sent: Drowsy-driver assistance requested for user at "
                + System.currentTimeMillis()
                + " (replace with real SMS/emergency flow).";
        Log.i(LOG_SMS_TAG, msg);
    }

    private void setStatus(String s) {
        if (statusText != null) {
            statusText.setText(s);
        }
        Log.d(TAG, "UI: " + s.replace('\n', ' '));
    }

    private void showOverlay(boolean show) {
        if (overlay != null) {
            overlay.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    private void showEscalationUi(boolean show) {
        if (escalationCountdown != null) {
            escalationCountdown.setVisibility(show ? View.VISIBLE : View.GONE);
        }
        if (btnCancelEmergency != null) {
            btnCancelEmergency.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

}
