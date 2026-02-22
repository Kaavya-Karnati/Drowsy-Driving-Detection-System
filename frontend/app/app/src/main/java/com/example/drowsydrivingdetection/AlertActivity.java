package com.example.drowsydrivingdetection;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AlertActivity extends AppCompatActivity {
    private TextView visualAlertPopup;
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler(Looper.getMainLooper()); //associated with a single thread and that thread's message queue
    /** There are two main uses for a Handler:
     * (1) to schedule messages and runnables to be executed at some point in the future; and
     * (2) to enqueue an action to be performed on a different thread than your own.**/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_system);

        //populating UI elements
        Button audioAlert = findViewById(R.id.audioAlert);
        Button visualAlert = findViewById(R.id.visualAlert);
        visualAlertPopup = findViewById(R.id.visualAlertPopUp);

        //setting on click events on buttons
        audioAlert.setOnClickListener(v -> playAudioAlert());
//        visualAlert.setOnClickListener(v -> showVisualAlert());
    }

    private void playAudioAlert() {
        //prevents overlap of sound
        if (this.mediaPlayer != null) {
            this.mediaPlayer.release(); //Release any previously playing audio (release resources too)
            this.mediaPlayer = null;
        }

        try {
            this.mediaPlayer = MediaPlayer.create(this, R.raw.audio_alert);
            this.mediaPlayer.start();

            this.mediaPlayer.setOnCompletionListener(mp -> { //callback function for when sound is done playing
                mp.release();
                this.mediaPlayer = null;
            });

        } catch (Exception err) { //for now catching all errors TODO:better error handling
            err.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (this.mediaPlayer != null) { //safety release of resources if still active
            this.mediaPlayer.release();
            this.mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null); //clear tasks from handler's queue
        //prevents tasks to be executed outside of this activity
    }
}

