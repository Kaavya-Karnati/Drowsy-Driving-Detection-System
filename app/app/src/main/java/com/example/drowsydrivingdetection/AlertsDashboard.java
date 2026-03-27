package com.example.drowsydrivingdetection;

import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;

import android.graphics.Color;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;

import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.Gson;

public class AlertsDashboard extends NavActivity{

    private TextView audioAlertDetectionText;
    private TextView visualAlertDetectionText;
    private ShapeableImageView profileIcon;

    private BarChart Bchart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_records);

        audioAlertDetectionText = findViewById(R.id.AuditoryAlertTotal);
        visualAlertDetectionText = findViewById(R.id.VisualAlertTotal);
        profileIcon = findViewById(R.id.profileIcon);

        runOnUiThread(() -> {
            if (getCurrentAudioAlertCounts() == 1) {
                audioAlertDetectionText.setText(getCurrentAudioAlertCounts() + " alert");
            } else {
                audioAlertDetectionText.setText(getCurrentAudioAlertCounts() + " alerts");
            }

            if (getCurrentVisualAlertCounts() == 1){
                visualAlertDetectionText.setText(getCurrentVisualAlertCounts() + " alert");
            } else {
                visualAlertDetectionText.setText(getCurrentVisualAlertCounts() + " alerts");
            }
        });

        logAlerts();
        setupBottomNavigation();
        loadProfilePicture();

        Bchart = findViewById(R.id.Barchart);
        chartman();
    }

    private int getCurrentAudioAlertCounts(){
        return sharedPreferences.getInt("audio_alert", 0);
    }

    private int getCurrentVisualAlertCounts(){
        return sharedPreferences.getInt("visual_alert", 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProfilePicture();
    }

    private void loadProfilePicture() {
        String profilePhoto = sharedPreferences.getString(getUserPhoto(), null);
        if (profilePhoto != null) {
            profileIcon.setImageURI(Uri.parse(profilePhoto));
        }
    }

    private String getUserPhoto() {
        String email = sharedPreferences.getString("userEmail", "guest");
        return "profilePhotoUri_" + email;
    }

    // Function to retrieve the alerts and return them as an array of Alert[]
    // - Anthony
    public Alert[] getAlertsArray() {
        Gson gson = new Gson();

        // Get the 'list' (it's gson list) from sharedPreferences
        String json = sharedPreferences.getString("all_alerts", null);

        // If it's a new user it'll be empty, so initialize a new list (same as DrowsinessTracker)
        if (json == null){
            return new Alert[0];
        }

        // Convert from json to actual array
        return gson.fromJson(json, Alert[].class);
    }

    // temporary function to make sure it's actually working, this will be removed later
    // - Anthony
    public void logAlerts() {
        Alert[] alerts = getAlertsArray();

        int totalAudio = 0;
        int totalVisual = 0;

        for (int i = 0; i < alerts.length; i++){
            Alert currentAlert = alerts[i];

            if(currentAlert.getAlertType().equals("audio")){
                totalAudio++;
            } else if (currentAlert.getAlertType().equals("visual")){
                totalVisual++;
            }
        }

        System.out.println("Visual: " + totalVisual);
        System.out.println("Audio: " + totalAudio);
    }

    private void chartman() {

        int aAlerts = getCurrentAudioAlertCounts();
        int vAlerts = getCurrentVisualAlertCounts();

        ArrayList<BarEntry> inputs = new ArrayList<>();
        inputs.add(new BarEntry(0, aAlerts));
        inputs.add(new BarEntry(1, vAlerts));

        BarDataSet dataset = new BarDataSet(inputs, "Weekly Alerts");

        dataset.setColors(
                Color.parseColor("#FBBC05"),
                Color.parseColor("#FE2C55")
        );

        dataset.setValueTextSize(14f);

        BarData Bdata = new BarData(dataset);
        Bdata.setBarWidth(0.6f);

        Bchart.setData(Bdata);
        String[] labels = {"Auditory", "Visual"};
        XAxis xAxis = Bchart.getXAxis();
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setGranularity(1f);
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        Bchart.getAxisRight().setEnabled(false);
        Bchart.getDescription().setEnabled(false);
        //Bchart.getDescription().setText("Audio vs Visual Alerts This Month");
        Bchart.getAxisLeft().setAxisMinimum(0f);
        Bchart.getAxisLeft().setGranularity(1f);
        Bchart.getAxisLeft().setGranularityEnabled(true);
        Bchart.getXAxis().setDrawGridLines(false);
        Bchart.setDrawGridBackground(false);
        Bchart.animateY(500);
        Bchart.invalidate();
    }

}
