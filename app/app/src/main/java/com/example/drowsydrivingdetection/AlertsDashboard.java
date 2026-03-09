package com.example.drowsydrivingdetection;

import android.os.Bundle;
import android.graphics.Color;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import java.util.ArrayList;

import androidx.appcompat.app.AppCompatActivity;

public class AlertsDashboard extends NavActivity{

    private BarChart Bchart;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_records);

        setupBottomNavigation();

        Bchart = findViewById(R.id.Bchart);
        chartman();
    }

    private void chartman() {


        int aAlerts = 5;
        int vAlerts = 1;

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
        Bchart.getDescription().setText("Audio vs Visual Alerts This Week");
        Bchart.invalidate();
    }
}



