package com.example.drowsydrivingdetection.voice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.ArrayList;

/**
 * This class is a wrapper around Android's SpeechRecognizer.
 * Its job is to handle all the logic of Android's speech API and give the rest of the app a simple
 * two-outcome interface: you either get a transcript, or you get a failure.
 * It also handles error-handling while using Google’s SpeechRecognizer class.
 */
public class SpeechRecognitionHelper {
    private static final String TAG = "SpeechRecognitionHelper";

    //safety timeout if the engine never calls us back (device-dependent)
    private static final long LISTEN_TIMEOUT_MS = 20_000;

    public interface RecognitionCallback {

        //called with the best transcript and the transcript's confidence score
        //runs on the main thread
        void onResult(String transcript, float confidence);

        //called when no speech was transcribed or detected withing the time limit
        //also called if there is an error, runs on main thread
        void onFailure(int errorCode, String errorDesc);
    }

    private final Context serviceContext;
    private final Handler mainHandler;
    private SpeechRecognizer recognizer;
    private RecognitionCallback pendingCallback;
    private boolean isListening = false;
    @Nullable
    private String lastPartialTranscript;


    private final Runnable timeoutRunnable = () -> {
        if (isListening) {
            Log.w(TAG, "Safety timeout fired: no result within " + LISTEN_TIMEOUT_MS + "ms");
            stopListening();
            deliverFailure(SpeechRecognizer.ERROR_SPEECH_TIMEOUT, "Timeout: no speech detected");
        }
    };

    public SpeechRecognitionHelper(Context context) {
        Context forService = context instanceof Activity ? context : context.getApplicationContext();
        if (!(context instanceof Activity)) {
            Log.w(TAG, "SpeechRecognitionHelper: pass an Activity for best speech to text reliability");
        }
        this.serviceContext = forService;
        this.mainHandler = new Handler(Looper.getMainLooper());

        if (!SpeechRecognizer.isRecognitionAvailable(serviceContext)) {
            Log.e(TAG, "SpeechRecognizer NOT available on this device");
        }
        initRecognizer();
    }

    /*
     function starts listening for when the question is asked and either onResult() or onFailure()
     is called. Must be called from the main UI thread.
    */
    public void listen(RecognitionCallback callback) {
        if (isListening) {
            Log.w(TAG, "listen() called while already listening -- IGNORING");
            return;
        }

        pendingCallback = callback;
        isListening = true;
        lastPartialTranscript = null;

        //Three setup steps before starting:
        //store the callback so the RecognitionListener can reach it,
        //set the boolean, and clear any leftover partial transcript from a previous session.
        Intent intent = buildRecognizerIntent();

        //ensure recognizer exists (may have been deleted after an error)
        if (recognizer == null) {
            initRecognizer();
        }

        recognizer.startListening(intent);
        Log.d(TAG, "Started listening");

        //arm the safety-net timeout
        mainHandler.postDelayed(timeoutRunnable, LISTEN_TIMEOUT_MS);

    //Starts the engine, then arms the 20-second safety timeout.
    // postDelayed schedules the runnable to run after 20,000ms on the main thread.
    // If results come in before then, the timeout gets cancelled via removeCallbacks()

    }

    /*
    Stops an in-progress recognition session without firing a callback.
     */
    public void cancel() {
        mainHandler.removeCallbacks(timeoutRunnable);
        if (recognizer != null) {
            recognizer.cancel();
        }
        isListening = false;
        pendingCallback = null;
        lastPartialTranscript = null;
        Log.d(TAG, "Recognition cancelled by caller");
    }

    /*
    release resources. Call from onDestroy().
     */
    public void destroy() {
        mainHandler.removeCallbacks(timeoutRunnable);
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        isListening = false;
        pendingCallback = null;
        Log.d(TAG, "SpeechRecognitionHelper destroyed");
    }

    private void initRecognizer() {
        recognizer = SpeechRecognizer.createSpeechRecognizer(serviceContext);
        recognizer.setRecognitionListener(new RecognitionListener() {
        //creates a fresh SpeechRecognizer instance and wires up an anonymous RecognitionListener.
        // The listener is defined inline as an anonymous class implementing all 9 required methods.

            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d(TAG, "Ready for speech");
            }
            //runs when the microphone is open, and the engine is actively listening.

            @Override
            public void onBeginningOfSpeech() {
                Log.d(TAG, "Speech beginning detected");
                //cancel safety-net if the user started speaking
                mainHandler.removeCallbacks(timeoutRunnable);
                //longer window to finish the utterance
                mainHandler.postDelayed(timeoutRunnable, LISTEN_TIMEOUT_MS + 8_000);
            }
            //runs the instant the engine detects audio that looks like speech.

            @Override
            public void onRmsChanged(float rmsdB) {}
            //onRmsChanged fires constantly with the microphone's volume level in decibels
            @Override
            public void onBufferReceived(byte[] buffer) {}
            //onBufferReceived provides raw audio bytes

            @Override
            public void onEndOfSpeech() {
                Log.d(TAG, "End of speech");
            }

            @Override
            public void onError(int error) {
                mainHandler.removeCallbacks(timeoutRunnable);
                //cancel the timeout (since we're done regardless of why), and clear the listening flag.
                //do not call stopListening() here since the session is already invalid
                isListening = false;

                String desc = errorDescription(error);
                Log.w(TAG, "Recognition error " + error + ": " + desc);

                destroyAndReinit();

                deliverFailure(error, desc);
            }

            @Override
            public void onResults(Bundle results) {
                mainHandler.removeCallbacks(timeoutRunnable);
                stopListening();
                //Cancel the timeout and clean up the session state.
                //stopListening() here is safe (unlike in onError) because
                // onResults means the engine completed normally.

                ArrayList<String> matches = results.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                float[] confidences = results.getFloatArray(
                        SpeechRecognizer.CONFIDENCE_SCORES);

                //Extract the results from the Bundle.
                //RESULTS_RECOGNITION gives you an ordered list of transcripts: index 0 is the engine's best guess,
                //and subsequent entries are alternative interpretations in decreasing confidence order.
                //CONFIDENCE_SCORES is a parallel float array where confidences[0] corresponds to matches.get(0).

                String best = (matches != null && !matches.isEmpty()) ? matches.get(0) : null;
                if (best != null) {
                    best = best.trim();
                    if (best.isEmpty()) {
                        best = null;
                    }
                }
                float bestConf = (confidences != null && confidences.length > 0)
                        ? confidences[0] : -1f;

                if (best == null && lastPartialTranscript != null && !lastPartialTranscript.isEmpty()) {
                    best = lastPartialTranscript;
                    if (bestConf < 0) {
                        bestConf = 0.45f;
                    }
                    Log.d(TAG, "Using partial fallback as final: \"" + best + "\"");
                }
                lastPartialTranscript = null;
                //If onResults gave us nothing but onPartialResults did capture something earlier,
                //use that partial transcript as the best guess, assigning it a confidence of 0.45 to signal it's lower-quality.

                Log.d(TAG, "Result: \"" + best + "\" conf=" + bestConf);
                deliverResult(best, bestConf);
            }

            @Override
            public void onPartialResults(Bundle partialResults) {
                ArrayList<String> partial = partialResults.getStringArrayList(
                        SpeechRecognizer.RESULTS_RECOGNITION);
                if (partial != null && !partial.isEmpty()) {
                    String p = partial.get(0);
                    if (p != null && !p.trim().isEmpty()) {
                        lastPartialTranscript = p.trim();
                        Log.d(TAG, "Partial: \"" + lastPartialTranscript + "\"");
                    }
                }
            }
            //runs repeatedly while the user is still speaking.
            //This class only stores the latest one in lastPartialTranscript.
            //Only the best partial (index 0) is kept

            @Override
            public void onEvent(int eventType, Bundle params) {}
            //needed to handle all 9 required  methods but no need for any implementation
        });
    }

    private Intent buildRecognizerIntent() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        //The action that tells Android to use speech recognition mode

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        //LANGUAGE_MODEL_FREE_FORM means the engine expects natural conversational speech.

        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US"); //Forces english
        intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, false);
        //Prefers cloud recognition over on-device recognition.
        // Cloud is more accurate but requires internet.
        // Setting this to false doesn't disable offline but it just prefers online if available

        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        //let the engine use default silence / segmenting since custom ms values caused
        //ERROR_NO_MATCH right after onBeginningOfSpeech on several devices.
        //enables partialResults

        return intent;
    }

    private void stopListening() {
        isListening = false;
        if (recognizer != null) {
            recognizer.stopListening();
        }
    }

    private void destroyAndReinit() {
        if (recognizer != null) {
            recognizer.destroy();
            recognizer = null;
        }
        //re-init on next listen() call
    }

    private void deliverResult(String transcript, float confidence) {
        RecognitionCallback callback = pendingCallback;
        pendingCallback = null;
        if (callback != null) {
            mainHandler.post(() -> callback.onResult(transcript, confidence));
        }
    }

    private void deliverFailure(int errorCode, String desc) {
        RecognitionCallback cb = pendingCallback;
        pendingCallback = null;
        if (cb != null) {
            mainHandler.post(() -> cb.onFailure(errorCode, desc));
        }
    }

    /*
    Map SpeechRecognizer error codes to human-readable strings for logs
     */
    private String errorDescription(int error) {
        switch (error) {
            case SpeechRecognizer.ERROR_AUDIO:
                return "Audio recording error (microphone unavailable?)";

            case SpeechRecognizer.ERROR_CLIENT:
                return "Client-side error";
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                return "Missing RECORD_AUDIO permission";

            case SpeechRecognizer.ERROR_NETWORK:
                return "Network error (offline mode may not be installed)";

            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                return "Network timeout";

            case SpeechRecognizer.ERROR_NO_MATCH:
                return "No speech match (silence, noise, or unclear audio — try again; online recognition works best)";

            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                return "Recognizer busy";

            case SpeechRecognizer.ERROR_SERVER:
                return "Server error (offline model may need installing)";

            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                return "No speech input detected within timeout";

            case SpeechRecognizer.ERROR_TOO_MANY_REQUESTS:
                return "Too many requests";

            default:
                return "Unknown error code: " + error;
        }
    }
}
