package com.example.drowsydrivingdetection.ui.alerts;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.example.drowsydrivingdetection.R;

public class AlertActivity extends AppCompatActivity {
    private TextView visualAlertPopup;
    private MediaPlayer mediaPlayer;
    private ImageView breakPopup;
    private ImageView closePopup;
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
        Button breakPopupBtn = findViewById(R.id.breakPopUpBtn);
        visualAlertPopup = findViewById(R.id.visualAlertPopUp);
        breakPopup = findViewById(R.id.breakPopUp);
        closePopup = findViewById(R.id.closePopup);


        //setting on click events on buttons
        audioAlert.setOnClickListener(v -> playAudioAlert());
        visualAlert.setOnClickListener(v -> showVisualAlert());
        breakPopupBtn.setOnClickListener(v -> showBreakPopup());
        closePopup.setOnClickListener(v -> closeBreakPopup());
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

    private void showVisualAlert() {
        handler.removeCallbacksAndMessages(null);
        //cancel any pending auto-hide

        visualAlertPopup.setAlpha(0f);
        visualAlertPopup.setVisibility(View.VISIBLE);
        //make popup text view visible

        // Fade the popup in
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(visualAlertPopup, "alpha", 0f, 1f);
        //turning the opacity to 1
        fadeIn.setDuration(400);
        fadeIn.start();

        //let's us run a task after a delay (get rid of popup after 3 seconds)
        //similar to setTimeout from Javascript
        handler.postDelayed(() -> {
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(visualAlertPopup, "alpha", 1f, 0f);
            //turning the opacity to 0

            fadeOut.setDuration(600);
            fadeOut.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    visualAlertPopup.setVisibility(View.GONE);
                }
            });
            //override the animation end fuction to set the popup to "gone"
            //or disappear from the XML

            fadeOut.start();
        }, 3000);

    }

    private void showBreakPopup() {
        handler.removeCallbacksAndMessages(null);
        //cancel any pending auto-hide

        breakPopup.setAlpha(0f);
        breakPopup.setVisibility(View.VISIBLE);
        closePopup.setVisibility(View.VISIBLE);
        //make popup text view visible
        //make close image view visible

        // Fade the popup in
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(breakPopup, "alpha", 0f, 1f);
        //turning the opacity to 1
        fadeIn.setDuration(400);
        fadeIn.start();

    }

    private void closeBreakPopup() {
        breakPopup.setAlpha(1f);
        breakPopup.setVisibility(View.GONE);
        closePopup.setVisibility(View.GONE);
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

