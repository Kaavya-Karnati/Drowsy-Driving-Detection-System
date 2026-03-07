package com.example.drowsydrivingdetection;

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

public class CleanDetectionResult {
    public List<BoundingBox> detections;
    public int eyesClosedCount;
    public int eyesOpenCount;
    public int yawnCount;
    public long timestamp;
    public float averageConfidence;

    public CleanDetectionResult() {
        this.detections = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    // Counts how many times each label (open/closed/yawn) appears in the detections list and calculates the average confidence score across all detections. 
    public void countDetections() {
        this.eyesClosedCount = 0;
        this.eyesOpenCount = 0;
        this.yawnCount = 0;
        float totalConfidence = 0;

        for(BoundingBox bbox : this.detections) {
            totalConfidence += bbox.confidence;

            switch (bbox.label.toLowerCase().replaceAll("\\s+", "")) {
                case "close":
                    eyesClosedCount++;
                    break;
                case "closed":
                    eyesClosedCount++;
                    break;
                case "open":
                    eyesOpenCount++;
                    break;
                case "yawn":
                    yawnCount++;
                    break;
                case "attentiveeye":
                    eyesOpenCount++;
                    break;
                case "drowsyeye":
                    eyesClosedCount++;
                    break;
            }
        }

        if (!this.detections.isEmpty()) {
            this.averageConfidence = totalConfidence / this.detections.size();
        }
    }

    // Checks if the driver is drowsy.
    public boolean isDrowsy() {
        return eyesClosedCount > 0 || yawnCount > 0;
    }

    // Converts the detection results into a readable string format for logging or display purposes.
    @SuppressLint("DefaultLocale")
    @NonNull
    @Override
    public String toString() {
        return String.format("Close: %d | Open: %d | Yawn: %d (conf: %.2f)",
                eyesClosedCount, eyesOpenCount, yawnCount, averageConfidence);
    }



}
