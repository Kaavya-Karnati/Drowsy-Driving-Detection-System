package com.example.drowsydrivingdetection.voice;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;

import java.util.Locale;

public class TTSHelper {
    private static final String TAG = "TTSHelper";
    private static final String UTTERANCE_ID = "voice_feedback_question";

    //question the app asks the driver
    public static final String DRIVER_PROMPT = "Are you alert enough to drive? Please say Yes, or say Get Help.";

    public interface OnSpeechDoneListener {

        //the file mentions "utterance" throughout, this just means the siri-type question prompt
        //runs on the main thread when the utterance (question prompt) has finished playing
        void onDone();

        //runs if TTS initialisation or playback fails
        void onError(String message);
    }

    private final Context context; //store as application context object
    private final AudioManager audioManager; //system audio file manager
    private TextToSpeech tts; //TTS engine instance
    private boolean ttsReady = false;

    //stored until TTS init completes (in case speak() is called before onInit)
    private String pendingText;
    private OnSpeechDoneListener pendingListener;
    //if speak() is called before ttsReady is true, the text and listener get parked here.
    // Once initialization completes, the queued call is automatically executed.

    public TTSHelper(Context context) {
        this.context = context.getApplicationContext(); //store the ApplicationContext
        //since TTS can outlive an Activity (it finishes async) holding a reference to an Activity context
        //would cause a memory leak if the Activity is destroyed before TTS init completes
        this.audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        initTTS();
        //by the time a caller uses TTSHelper, it's already been initializing in the background.
    }

    //speak the standard driver-check prompt
    //if TTS is not yet initialised the call is queued and runs once ready
    public void speakDriverPrompt(OnSpeechDoneListener listener) {
        speak(DRIVER_PROMPT, listener);
    }


    //speak any text. safe to call before TTS init completes since the text is queued.
    public void speak(String text, OnSpeechDoneListener listener) {
        if (!ttsReady) {
            //queue for when onInit runs
            pendingText = text;
            pendingListener = listener;
            Log.d(TAG, "TTS not ready yet -- queuing utterance");
            return;
        }
        //only play the prompt if TTS engine is ready
        playUtterance(text, listener);
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop(); //stops any audio playing
            tts.shutdown(); //releases the TTS engine
            tts = null;
        }
        ttsReady = false;
        Log.d(TAG, "TTSHelper shut down");
    }

    private void initTTS() {
        tts = new TextToSpeech(context, status -> {
            //create the TTS engine here

            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.US);

                if (result == TextToSpeech.LANG_MISSING_DATA
                        || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e(TAG, "TTS language not supported");

                    if (pendingListener != null) {
                        pendingListener.onError("TTS language not supported");
                    }
                    return;
                } //if language pack missing on the device or unsupported language packs, we send an
                //error if any of the conditions are true

                tts.setSpeechRate(0.9f); //slightly slower speak rate since it is easier to hear while driving
                tts.setPitch(1.0f);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int usage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                            ? AudioAttributes.USAGE_ASSISTANT
                            : AudioAttributes.USAGE_VOICE_COMMUNICATION;
                    tts.setAudioAttributes(new AudioAttributes.Builder()
                            .setUsage(usage)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build());
                }

                ttsReady = true;
                Log.d(TAG, "TTS engine ready");

                //if speak() was called before init finished, run it now
                if (pendingText != null && pendingListener != null) {
                    playUtterance(pendingText, pendingListener);
                    pendingText = null;
                    pendingListener = null;
                }
            } else { //if TTS engine failed to initialize
                Log.e(TAG, "TTS init failed: " + status);

                if (pendingListener != null) {
                    pendingListener.onError("TTS initialisation failed");
                }
            }
        });
    }

    private void playUtterance(String text, OnSpeechDoneListener listener) {
        //request audio focus so TTS doesn't compete with music / alerts
        //android gives this priority in audio stack stuff (when music stops on waze alerts)
        requestAudioFocus();

        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                Log.d(TAG, "TTS started: " + utteranceId);
            }

            @Override
            public void onDone(String utteranceId) {
                Log.d(TAG, "TTS done: " + utteranceId);
                abandonAudioFocus();
                if (listener != null) {
                    listener.onDone(); //audio finished playing and now we can listen for user response
                }
            }

            @Override
            public void onError(String utteranceId) {
                Log.e(TAG, "TTS error for utterance: " + utteranceId);
                abandonAudioFocus();
                if (listener != null) {
                    listener.onError("TTS playback error");
                }
            }
        });


        android.os.Bundle params = new android.os.Bundle();
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC);
        } //not too sure about "lollipop" code but this is needed to handle pre-lollipop devices

        //QUEUE_FLUSH stops any current speech immediately and plays this
        int result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, UTTERANCE_ID);
        if (result == TextToSpeech.ERROR) {
            Log.e(TAG, "tts.speak() returned ERROR");
            abandonAudioFocus();
            if (listener != null) {
                listener.onError("tts.speak() returned ERROR");
            }
        }
    }

    private void requestAudioFocus() {
        if (audioManager != null) {
            audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            );
        }
    }

    private void abandonAudioFocus() {
        if (audioManager != null) {
            audioManager.abandonAudioFocus(null);
        }
    }
}
